package com.myapp.ui.components;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public final class KeyboardShortcutManager {

    private KeyboardShortcutManager() {}

    public static void register(JFrame frame, int keyCode, int modifiers, Runnable action) {
        String actionName = "shortcut_" + keyCode + "_" + modifiers;
        JRootPane rootPane = frame.getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyCode, modifiers), actionName);
        rootPane.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    public static void registerDashboardShortcuts(JFrame frame, Runnable openStock,
            Runnable openFacture, Runnable openBL, Runnable openClients,
            Runnable openFournisseurs, Runnable openDevis, Runnable openSearch) {
        register(frame, KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, openStock);
        register(frame, KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK, openFacture);
        register(frame, KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK, openBL);
        register(frame, KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK, openClients);
        register(frame, KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK, openFournisseurs);
        register(frame, KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK, openDevis);
        register(frame, KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK, openSearch);
        register(frame, KeyEvent.VK_ESCAPE, 0, () -> {});
    }
}
