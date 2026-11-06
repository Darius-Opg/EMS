package com.ems.auth.ui;

import com.ems.shared.model.UserSession;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Loads module panels once their module owners provide the documented panel classes. */
final class ModulePanelLoader {

    private ModulePanelLoader() {
    }

    static JPanel load(String panelClassName, UserSession session, String unavailableMessage,
                       String... serviceClassNames) {
        try {
            Class<?> panelClass = Class.forName(panelClassName);
            if (!JPanel.class.isAssignableFrom(panelClass)) {
                return unavailable(unavailableMessage);
            }

            List<Object> services = createServices(serviceClassNames);
            Constructor<?>[] constructors = panelClass.getConstructors();
            Arrays.sort(constructors,
                    (left, right) -> Integer.compare(right.getParameterCount(), left.getParameterCount()));
            for (Constructor<?> constructor : constructors) {
                Object[] arguments = resolveArguments(constructor.getParameterTypes(), session, services);
                if (arguments == null) continue;
                try {
                    return (JPanel) constructor.newInstance(arguments);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Try another supported public constructor before showing the placeholder.
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // Module panels are optional until their implementation is available.
        }
        return unavailable(unavailableMessage);
    }

    private static List<Object> createServices(String[] classNames) {
        List<Object> services = new ArrayList<>();
        for (String className : classNames) {
            try {
                Class<?> serviceClass = Class.forName(className);
                services.add(serviceClass.getConstructor().newInstance());
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                // A panel may not need every service listed for its module.
            }
        }
        return services;
    }

    private static Object[] resolveArguments(Class<?>[] parameterTypes, UserSession session,
                                             List<Object> services) {
        Object[] arguments = new Object[parameterTypes.length];
        boolean[] usedServices = new boolean[services.size()];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.isInstance(session)) {
                arguments[i] = session;
                continue;
            }
            int serviceIndex = -1;
            for (int j = 0; j < services.size(); j++) {
                if (!usedServices[j] && parameterType.isInstance(services.get(j))) {
                    serviceIndex = j;
                    break;
                }
            }
            if (serviceIndex < 0) return null;
            usedServices[serviceIndex] = true;
            arguments[i] = services.get(serviceIndex);
        }
        return arguments;
    }

    private static JPanel unavailable(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new javax.swing.JLabel(message, javax.swing.JLabel.CENTER), BorderLayout.CENTER);
        return panel;
    }
}
