package com.myapp.ui.components;

import com.myapp.util.AppTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Systeme de notifications toast pour l'interface Swing.
 * Les notifications apparaissent en haut a droite de la fenetre parente
 * et disparaissent automatiquement apres 3 secondes avec un effet de fondu.
 */
public final class NotificationManager {

    private static final Logger log = LoggerFactory.getLogger(NotificationManager.class);

    private static final int TOAST_WIDTH = 350;
    private static final int TOAST_HEIGHT = 50;
    private static final int MARGIN = 15;
    private static final int GAP = 8;
    private static final int ARC = 18;
    private static final int DISPLAY_DURATION_MS = 3000;
    private static final int FADE_STEP_MS = 30;
    private static final float FADE_DECREMENT = 0.05f;

    private static final String ICON_SUCCESS = "✔";
    private static final String ICON_WARNING = "⚠";
    private static final String ICON_ERROR = "✖";
    private static final String ICON_INFO = "ℹ";

    private static final List<JPanel> activeToasts = new ArrayList<>();

    private NotificationManager() {}

    public static void showSuccess(Component parent, String message) {
        show(parent, message, AppTheme.ACCENT, ICON_SUCCESS);
    }

    public static void showWarning(Component parent, String message) {
        show(parent, message, AppTheme.WARNING, ICON_WARNING);
    }

    public static void showError(Component parent, String message) {
        show(parent, message, AppTheme.DANGER, ICON_ERROR);
    }

    public static void showInfo(Component parent, String message) {
        show(parent, message, AppTheme.INFO, ICON_INFO);
    }

    private static void show(Component parent, String message, Color bgColor, String icon) {
        SwingUtilities.invokeLater(() -> {
            try {
                Window window = getWindow(parent);
                if (window == null) {
                    log.warn("Impossible d'afficher la notification : fenetre parente introuvable");
                    return;
                }

                JLayeredPane layeredPane = getLayeredPane(window);
                if (layeredPane == null) {
                    log.warn("Impossible d'afficher la notification : layered pane introuvable");
                    return;
                }

                JPanel toast = createToastPanel(bgColor, icon, message, layeredPane);
                synchronized (activeToasts) {
                    activeToasts.add(toast);
                }

                layeredPane.add(toast, JLayeredPane.POPUP_LAYER);
                repositionToasts(layeredPane);
                layeredPane.revalidate();
                layeredPane.repaint();

                scheduleAutoDismiss(toast, layeredPane);
                log.debug("Notification affichee : {}", message);
            } catch (Exception e) {
                log.error("Erreur lors de l'affichage de la notification", e);
            }
        });
    }

    private static JPanel createToastPanel(Color bgColor, String icon, String message, JLayeredPane layeredPane) {
        JPanel toast = new JPanel(new BorderLayout(8, 0)) {
            private float opacity = 1.0f;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                g2.setColor(bgColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                g2.dispose();
            }

            public void setOpacity(float opacity) {
                this.opacity = opacity;
                repaint();
            }

            public float getOpacity() {
                return opacity;
            }
        };

        toast.setOpaque(false);
        toast.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 10));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        iconLabel.setForeground(Color.WHITE);
        toast.add(iconLabel, BorderLayout.WEST);

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageLabel.setForeground(Color.WHITE);
        toast.add(messageLabel, BorderLayout.CENTER);

        JLabel closeBtn = new JLabel("✕");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeBtn.setForeground(new Color(255, 255, 255, 180));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dismissToast(toast, layeredPane);
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                closeBtn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                closeBtn.setForeground(new Color(255, 255, 255, 180));
            }
        });
        toast.add(closeBtn, BorderLayout.EAST);

        return toast;
    }

    private static void scheduleAutoDismiss(JPanel toast, JLayeredPane layeredPane) {
        Timer dismissTimer = new Timer(DISPLAY_DURATION_MS, e -> fadeOut(toast, layeredPane));
        dismissTimer.setRepeats(false);
        dismissTimer.start();
    }

    private static void fadeOut(JPanel toast, JLayeredPane layeredPane) {
        Timer fadeTimer = new Timer(FADE_STEP_MS, null);
        fadeTimer.addActionListener(new ActionListener() {
            float opacity = 1.0f;

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity -= FADE_DECREMENT;
                if (opacity <= 0) {
                    fadeTimer.stop();
                    dismissToast(toast, layeredPane);
                } else {
                    try {
                        java.lang.reflect.Method m = toast.getClass().getMethod("setOpacity", float.class);
                        m.invoke(toast, opacity);
                    } catch (Exception ex) {
                        // fallback: just remove
                        fadeTimer.stop();
                        dismissToast(toast, layeredPane);
                    }
                }
            }
        });
        fadeTimer.start();
    }

    private static void dismissToast(JPanel toast, JLayeredPane layeredPane) {
        SwingUtilities.invokeLater(() -> {
            synchronized (activeToasts) {
                activeToasts.remove(toast);
            }
            layeredPane.remove(toast);
            repositionToasts(layeredPane);
            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }

    private static void repositionToasts(JLayeredPane layeredPane) {
        synchronized (activeToasts) {
            int y = MARGIN;
            int x = layeredPane.getWidth() - TOAST_WIDTH - MARGIN;
            if (x < 0) x = MARGIN;
            for (JPanel toast : activeToasts) {
                toast.setBounds(x, y, TOAST_WIDTH, TOAST_HEIGHT);
                y += TOAST_HEIGHT + GAP;
            }
        }
    }

    private static Window getWindow(Component comp) {
        if (comp instanceof Window) {
            return (Window) comp;
        }
        return SwingUtilities.getWindowAncestor(comp);
    }

    private static JLayeredPane getLayeredPane(Window window) {
        if (window instanceof JFrame) {
            return ((JFrame) window).getLayeredPane();
        } else if (window instanceof JDialog) {
            return ((JDialog) window).getLayeredPane();
        }
        return null;
    }
}
