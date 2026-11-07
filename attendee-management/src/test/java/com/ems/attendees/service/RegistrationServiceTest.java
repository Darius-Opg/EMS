/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.ems.attendees.dao.AttendeeDAO
 *  com.ems.attendees.dao.RegistrationCapacityDAO
 *  com.ems.attendees.dao.RegistrationCapacityDAO$CapacitySnapshot
 *  com.ems.attendees.model.Attendee
 *  com.ems.attendees.model.VipAttendee
 *  com.ems.attendees.service.RegistrationService
 *  com.ems.attendees.service.RegistrationService$RegistrationException
 *  com.ems.attendees.service.RegistrationService$RegistrationResult
 *  com.ems.shared.model.UserSession
 *  com.ems.tickets.model.Ticket
 *  com.ems.tickets.service.TicketService
 *  org.junit.jupiter.api.Assertions
 *  org.junit.jupiter.api.BeforeEach
 *  org.junit.jupiter.api.Test
 *  org.mockito.ArgumentMatchers
 *  org.mockito.Mockito
 *  org.mockito.verification.VerificationMode
 *  org.postgresql.util.PSQLException
 *  org.postgresql.util.ServerErrorMessage
 */
package com.ems.attendees.service;

import com.ems.attendees.dao.AttendeeDAO;
import com.ems.attendees.dao.RegistrationCapacityDAO;
import com.ems.attendees.model.Attendee;
import com.ems.attendees.model.VipAttendee;
import com.ems.attendees.service.RegistrationService;
import com.ems.shared.model.UserSession;
import com.ems.tickets.model.Ticket;
import com.ems.tickets.service.TicketService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

class RegistrationServiceTest {
    private RegistrationService service;
    private AttendeeDAO attendeeDAO;
    private RegistrationCapacityDAO capacityDAO;
    private TicketService ticketService;
    private Connection connection;
    private PreparedStatement eventStatement;
    private ResultSet eventResults;
    private AtomicBoolean connectionRequested;
    private UserSession adminSession;
    private Ticket issuedTicket;

    RegistrationServiceTest() {
    }

    @BeforeEach
    void setUp() throws SQLException {
        this.connectionRequested = new AtomicBoolean(false);
        this.attendeeDAO = (AttendeeDAO)Mockito.mock(AttendeeDAO.class);
        this.capacityDAO = (RegistrationCapacityDAO)Mockito.mock(RegistrationCapacityDAO.class);
        this.ticketService = (TicketService)Mockito.mock(TicketService.class);
        this.connection = (Connection)Mockito.mock(Connection.class);
        this.eventStatement = (PreparedStatement)Mockito.mock(PreparedStatement.class);
        this.eventResults = (ResultSet)Mockito.mock(ResultSet.class);
        this.adminSession = this.session("ADMIN");
        this.issuedTicket = new Ticket(100L, 42L, "EMS-TEST-123456", "VALID", OffsetDateTime.now(), null, null, "Jane Doe", "jane@example.com", "0712345678", false, Long.valueOf(1L), "Test Event");
        Mockito.when((Object)this.connection.prepareStatement(ArgumentMatchers.anyString())).thenReturn((Object)this.eventStatement);
        Mockito.when((Object)this.eventStatement.executeQuery()).thenReturn((Object)this.eventResults);
        Mockito.when((Object)this.eventResults.next()).thenReturn((Object)true);
        Mockito.when((Object)this.eventResults.getString("status")).thenReturn((Object)"UPCOMING");
        Mockito.when((Object)this.capacityDAO.lockAndGetCapacity(this.connection, 1L)).thenReturn((Object)new RegistrationCapacityDAO.CapacitySnapshot(100L, 0L));
        Mockito.when((Object)this.attendeeDAO.insert((Connection)ArgumentMatchers.eq((Object)this.connection), (Attendee)Mockito.any(Attendee.class))).thenReturn((Object)42L);
        Mockito.when((Object)this.ticketService.issueTicket(this.connection, 42L, this.adminSession)).thenReturn((Object)this.issuedTicket);
        this.service = new RegistrationService(this.attendeeDAO, this.capacityDAO, this.ticketService, () -> {
            this.connectionRequested.set(true);
            return this.connection;
        });
    }

    private SQLException uniqueViolation(String constraintName) {
        String errorFields =
                "SERROR\0"
                + "C23505\0"
                + "Mduplicate key value violates unique constraint\0"
                + "n" + constraintName + "\0"
                + "\0";

        ServerErrorMessage serverError =
                new ServerErrorMessage(errorFields);

        return new PSQLException(serverError, true);
    }

    private UserSession session(String role) {
        return new UserSession(1L, "Test User", "testuser", role);
    }

    private Attendee validAttendee() {
        return new Attendee(Long.valueOf(1L), "Jane Doe", "jane@example.com", "0712345678");
    }

    @Test
    void rejectsNullAttendee() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.register(null, this.adminSession));
        Assertions.assertFalse((boolean)this.connectionRequested.get());
    }

    @Test
    void rejectsMissingEventId() {
        Attendee attendee = this.validAttendee();
        attendee.setEventId(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.register(attendee, this.adminSession));
        Assertions.assertFalse((boolean)this.connectionRequested.get());
    }

    @Test
    void rejectsNonPositiveEventId() {
        Attendee attendee = this.validAttendee();
        attendee.setEventId(Long.valueOf(0L));
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.register(attendee, this.adminSession));
        Assertions.assertFalse((boolean)this.connectionRequested.get());
    }

    @Test
    void rejectsBlankAttendeeName() {
        Attendee attendee = this.validAttendee();
        attendee.setFullName("   ");
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.register(attendee, this.adminSession));
        Assertions.assertFalse((boolean)this.connectionRequested.get());
    }

    @Test
    void rejectsInvalidEmailAddress() {
        Attendee attendee = this.validAttendee();
        attendee.setEmail("not-an-email");
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.register(attendee, this.adminSession));
        Assertions.assertFalse((boolean)this.connectionRequested.get());
    }

    @Test
    void rejectsInvalidPhoneNumber() {
        Attendee attendee = this.validAttendee();
        attendee.setPhone("not-a-phone");
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.register(attendee, this.adminSession));
        Assertions.assertFalse((boolean)this.connectionRequested.get());
    }

    @Test
    void rejectsVipAttendeeWithoutVipLevel() {
        VipAttendee attendee = new VipAttendee(Long.valueOf(1L), "Jane Doe", "jane@example.com", "0712345678", "   ", "Wheelchair access");
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.register((Attendee)attendee, this.adminSession));
        Assertions.assertFalse((boolean)this.connectionRequested.get());
    }

    @Test
    void rejectsUnauthorizedRoleBeforeDatabaseAccess() {
        Assertions.assertThrows(SecurityException.class, () -> this.service.register(this.validAttendee(), this.session("VIEWER")));
        Assertions.assertFalse((boolean)this.connectionRequested.get());
    }

    @Test
    void rejectsNullSessionBeforeDatabaseAccess() {
        Assertions.assertThrows(SecurityException.class, () -> this.service.register(this.validAttendee(), null));
        Assertions.assertFalse((boolean)this.connectionRequested.get());
    }

    @Test
    void normalizesValidVipDetailsBeforeRegistration() {
        VipAttendee attendee = new VipAttendee(Long.valueOf(1L), "  Jane Doe  ", " JANE@EXAMPLE.COM ", " 0712345678 ", "  GOLD  ", "  Wheelchair access  ");
        RegistrationService.RegistrationResult result = this.service.register((Attendee)attendee, this.adminSession);
        Assertions.assertNotNull((Object)result);
        Assertions.assertEquals((Object)"Jane Doe", (Object)attendee.getFullName());
        Assertions.assertEquals((Object)"jane@example.com", (Object)attendee.getEmail());
        Assertions.assertEquals((Object)"0712345678", (Object)attendee.getPhone());
        Assertions.assertEquals((Object)"GOLD", (Object)attendee.getVipLevel());
        Assertions.assertEquals((Object)"Wheelchair access", (Object)attendee.getSpecialRequirements());
    }

    @Test
    void registersAttendeeIssuesTicketAndCommitsTransaction() throws SQLException {
        Attendee attendee = this.validAttendee();
        RegistrationService.RegistrationResult result = this.service.register(attendee, this.adminSession);
        Assertions.assertNotNull((Object)result);
        Assertions.assertSame((Object)attendee, (Object)result.attendee());
        Assertions.assertSame((Object)this.issuedTicket, (Object)result.ticket());
        ((Connection)Mockito.verify((Object)this.connection)).setAutoCommit(false);
        ((AttendeeDAO)Mockito.verify((Object)this.attendeeDAO)).insert(this.connection, attendee);
        ((TicketService)Mockito.verify((Object)this.ticketService)).issueTicket(this.connection, 42L, this.adminSession);
        ((Connection)Mockito.verify((Object)this.connection)).commit();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).rollback();
    }

    @Test
    void rejectsRegistrationWhenNoRoomIsAssigned() throws SQLException {
        Mockito.when((Object)this.capacityDAO.lockAndGetCapacity(this.connection, 1L)).thenReturn(null);
        RegistrationService.RegistrationException exception = (RegistrationService.RegistrationException)Assertions.assertThrows(RegistrationService.RegistrationException.class, () -> this.service.register(this.validAttendee(), this.adminSession));
        Assertions.assertTrue((boolean)exception.getMessage().contains("assigned room"));
        ((AttendeeDAO)Mockito.verify((Object)this.attendeeDAO, (VerificationMode)Mockito.never())).insert((Connection)Mockito.any(Connection.class), (Attendee)Mockito.any(Attendee.class));
        ((TicketService)Mockito.verify((Object)this.ticketService, (VerificationMode)Mockito.never())).issueTicket((Connection)Mockito.any(Connection.class), Mockito.anyLong(), (UserSession)Mockito.any(UserSession.class));
        ((Connection)Mockito.verify((Object)this.connection)).rollback();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).commit();
    }

    @Test
    void rejectsRegistrationWhenRoomCapacityIsReached() throws SQLException {
        Mockito.when((Object)this.capacityDAO.lockAndGetCapacity(this.connection, 1L)).thenReturn((Object)new RegistrationCapacityDAO.CapacitySnapshot(50L, 50L));
        RegistrationService.RegistrationException exception = (RegistrationService.RegistrationException)Assertions.assertThrows(RegistrationService.RegistrationException.class, () -> this.service.register(this.validAttendee(), this.adminSession));
        Assertions.assertTrue((boolean)exception.getMessage().contains("Registration is full"));
        ((AttendeeDAO)Mockito.verify((Object)this.attendeeDAO, (VerificationMode)Mockito.never())).insert((Connection)Mockito.any(Connection.class), (Attendee)Mockito.any(Attendee.class));
        ((TicketService)Mockito.verify((Object)this.ticketService, (VerificationMode)Mockito.never())).issueTicket((Connection)Mockito.any(Connection.class), Mockito.anyLong(), (UserSession)Mockito.any(UserSession.class));
        ((Connection)Mockito.verify((Object)this.connection)).rollback();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).commit();
    }

    @Test
    void rejectsRegistrationForCompletedEvent() throws SQLException {
        Mockito.when((Object)this.eventResults.getString("status")).thenReturn((Object)"COMPLETED");
        RegistrationService.RegistrationException exception = (RegistrationService.RegistrationException)Assertions.assertThrows(RegistrationService.RegistrationException.class, () -> this.service.register(this.validAttendee(), this.adminSession));
        Assertions.assertTrue((boolean)exception.getMessage().contains("Registration is not available"));
        ((RegistrationCapacityDAO)Mockito.verify((Object)this.capacityDAO, (VerificationMode)Mockito.never())).lockAndGetCapacity((Connection)Mockito.any(Connection.class), Mockito.anyLong());
        ((AttendeeDAO)Mockito.verify((Object)this.attendeeDAO, (VerificationMode)Mockito.never())).insert((Connection)Mockito.any(Connection.class), (Attendee)Mockito.any(Attendee.class));
        ((Connection)Mockito.verify((Object)this.connection)).rollback();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).commit();
    }

    @Test
    void rollsBackAttendeeInsertWhenTicketIssuanceFails() throws SQLException {
        Mockito.when((Object)this.ticketService.issueTicket(this.connection, 42L, this.adminSession)).thenThrow(new Throwable[]{new SQLException("Ticket persistence failed.")});
        RegistrationService.RegistrationException exception = (RegistrationService.RegistrationException)Assertions.assertThrows(RegistrationService.RegistrationException.class, () -> this.service.register(this.validAttendee(), this.adminSession));
        Assertions.assertTrue((boolean)exception.getMessage().contains("Registration could not be completed"));
        ((AttendeeDAO)Mockito.verify((Object)this.attendeeDAO)).insert((Connection)ArgumentMatchers.eq((Object)this.connection), (Attendee)Mockito.any(Attendee.class));
        ((TicketService)Mockito.verify((Object)this.ticketService)).issueTicket(this.connection, 42L, this.adminSession);
        ((Connection)Mockito.verify((Object)this.connection)).rollback();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).commit();
    }

    @Test
    void updatesAttendeeAndCommitsTransaction() throws SQLException {
        Attendee attendee = this.validAttendee();
        attendee.setId(Long.valueOf(42L));
        attendee.setFullName("Jane Updated");
        Mockito.when((Object)this.eventResults.getLong("event_id")).thenReturn((Object)1L);
        Mockito.when((Object)this.attendeeDAO.update(this.connection, attendee)).thenReturn((Object)true);
        Attendee result = this.service.updateAttendee(attendee, this.adminSession);
        Assertions.assertSame((Object)attendee, (Object)result);
        ((AttendeeDAO)Mockito.verify((Object)this.attendeeDAO)).update(this.connection, attendee);
        ((Connection)Mockito.verify((Object)this.connection)).commit();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).rollback();
    }

    @Test
    void preventsUpdatingAttendeeIntoAnotherEvent() throws SQLException {
        Attendee attendee = this.validAttendee();
        attendee.setId(Long.valueOf(42L));
        attendee.setEventId(Long.valueOf(2L));
        Mockito.when((Object)this.eventResults.getLong("event_id")).thenReturn((Object)1L);
        RegistrationService.RegistrationException exception = (RegistrationService.RegistrationException)Assertions.assertThrows(RegistrationService.RegistrationException.class, () -> this.service.updateAttendee(attendee, this.adminSession));
        Assertions.assertTrue((boolean)exception.getMessage().contains("cannot be transferred"));
        ((AttendeeDAO)Mockito.verify((Object)this.attendeeDAO, (VerificationMode)Mockito.never())).update((Connection)Mockito.any(Connection.class), (Attendee)Mockito.any(Attendee.class));
        ((Connection)Mockito.verify((Object)this.connection)).rollback();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).commit();
    }

    @Test
    void rollsBackWhenAttendeeUpdateReturnsFalse() throws SQLException {
        Attendee attendee = this.validAttendee();
        attendee.setId(Long.valueOf(42L));
        Mockito.when((Object)this.eventResults.getLong("event_id")).thenReturn((Object)1L);
        Mockito.when((Object)this.attendeeDAO.update(this.connection, attendee)).thenReturn((Object)false);
        RegistrationService.RegistrationException exception = (RegistrationService.RegistrationException)Assertions.assertThrows(RegistrationService.RegistrationException.class, () -> this.service.updateAttendee(attendee, this.adminSession));
        Assertions.assertTrue((boolean)exception.getMessage().contains("could not be updated"));
        ((AttendeeDAO)Mockito.verify((Object)this.attendeeDAO)).update(this.connection, attendee);
        ((Connection)Mockito.verify((Object)this.connection)).rollback();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).commit();
    }

    @Test
    void deletesAttendeeAndCommitsTransaction() throws SQLException {
        Mockito.when((Object)this.eventResults.getLong("event_id")).thenReturn((Object)1L);
        Mockito.when((Object)this.attendeeDAO.delete(this.connection, 42L)).thenReturn((Object)true);
        this.service.deleteAttendee(42L, 1L, this.adminSession);
        ((AttendeeDAO)Mockito.verify((Object)this.attendeeDAO)).delete(this.connection, 42L);
        ((Connection)Mockito.verify((Object)this.connection)).commit();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).rollback();
    }

    @Test
    void preventsDeletingAttendeeWithIssuedTicket() throws SQLException {
        Mockito.when((Object)this.eventResults.getLong("event_id")).thenReturn((Object)1L);
        Mockito.when((Object)this.attendeeDAO.delete(this.connection, 42L)).thenThrow(new Throwable[]{new SQLException("Foreign key constraint violation.", "23503")});
        RegistrationService.RegistrationException exception = (RegistrationService.RegistrationException)Assertions.assertThrows(RegistrationService.RegistrationException.class, () -> this.service.deleteAttendee(42L, 1L, this.adminSession));
        Assertions.assertTrue((boolean)exception.getMessage().contains("issued ticket"));
        Assertions.assertTrue((boolean)exception.getMessage().contains("attendance history"));
        ((AttendeeDAO)Mockito.verify((Object)this.attendeeDAO)).delete(this.connection, 42L);
        ((Connection)Mockito.verify((Object)this.connection)).rollback();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).commit();
    }

    @Test
    void preservesRegistrationSuccessWhenConnectionCloseFails() throws SQLException {
        ((Connection)Mockito.doThrow((Throwable[])new Throwable[]{new SQLException("Connection close failed.")}).when((Object)this.connection)).close();
        RegistrationService.RegistrationResult result = this.service.register(this.validAttendee(), this.adminSession);
        Assertions.assertNotNull((Object)result);
        Assertions.assertSame((Object)this.issuedTicket, (Object)result.ticket());
        ((Connection)Mockito.verify((Object)this.connection)).commit();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).rollback();
    }

    @Test
    void preservesUpdateSuccessWhenConnectionCloseFails() throws SQLException {
        Attendee attendee = this.validAttendee();
        attendee.setId(Long.valueOf(42L));
        Mockito.when((Object)this.eventResults.getLong("event_id")).thenReturn((Object)1L);
        Mockito.when((Object)this.attendeeDAO.update(this.connection, attendee)).thenReturn((Object)true);
        ((Connection)Mockito.doThrow((Throwable[])new Throwable[]{new SQLException("Connection close failed.")}).when((Object)this.connection)).close();
        Attendee result = this.service.updateAttendee(attendee, this.adminSession);
        Assertions.assertSame((Object)attendee, (Object)result);
        ((Connection)Mockito.verify((Object)this.connection)).commit();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).rollback();
    }

    @Test
    void preservesDeleteSuccessWhenConnectionCloseFails() throws SQLException {
        Mockito.when((Object)this.eventResults.getLong("event_id")).thenReturn((Object)1L);
        Mockito.when((Object)this.attendeeDAO.delete(this.connection, 42L)).thenReturn((Object)true);
        ((Connection)Mockito.doThrow((Throwable[])new Throwable[]{new SQLException("Connection close failed.")}).when((Object)this.connection)).close();
        Assertions.assertDoesNotThrow(() -> this.service.deleteAttendee(42L, 1L, this.adminSession));
        ((Connection)Mockito.verify((Object)this.connection)).commit();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).rollback();
    }

    @Test
    void identifiesAttendeeEmailUniqueViolationPrecisely() throws SQLException {
        Mockito.when((Object)this.attendeeDAO.insert((Connection)ArgumentMatchers.eq((Object)this.connection), (Attendee)Mockito.any(Attendee.class))).thenThrow(new Throwable[]{this.uniqueViolation("uq_attendees_event_email")});
        RegistrationService.RegistrationException exception = (RegistrationService.RegistrationException)Assertions.assertThrows(RegistrationService.RegistrationException.class, () -> this.service.register(this.validAttendee(), this.adminSession));
        Assertions.assertTrue((boolean)exception.getMessage().contains("already registered"));
        ((Connection)Mockito.verify((Object)this.connection)).rollback();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).commit();
    }

    @Test
    void doesNotMisreportTicketNumberUniqueViolationAsDuplicateEmail() throws SQLException {
        Mockito.when((Object)this.attendeeDAO.insert((Connection)ArgumentMatchers.eq((Object)this.connection), (Attendee)Mockito.any(Attendee.class))).thenThrow(new Throwable[]{this.uniqueViolation("tickets_ticket_number_key")});
        RegistrationService.RegistrationException exception = (RegistrationService.RegistrationException)Assertions.assertThrows(RegistrationService.RegistrationException.class, () -> this.service.register(this.validAttendee(), this.adminSession));
        Assertions.assertTrue((boolean)exception.getMessage().contains("Registration could not be completed"));
        Assertions.assertFalse((boolean)exception.getMessage().contains("already registered"));
        ((Connection)Mockito.verify((Object)this.connection)).rollback();
        ((Connection)Mockito.verify((Object)this.connection, (VerificationMode)Mockito.never())).commit();
    }
}
