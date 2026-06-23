/*    */ package com.myapp.ui.components;
/*    */ import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
/*    */ import javax.swing.BorderFactory;
/*    */ import javax.swing.JButton;
/*    */ import javax.swing.JPanel;
/*    */ import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
/*    */ 
/*    */ public class UIComponentFactory {
/*    */   public static JButton createStyledButton(String text, final Color backgroundColor) {
/* 10 */     final JButton button = new JButton(text);
/* 11 */     button.setFont(new Font("Segoe UI", 1, 12));
/* 12 */     button.setBackground(backgroundColor);
/* 13 */     button.setForeground(Color.WHITE);
/* 14 */     button.setFocusPainted(false);
/* 15 */     button.setBorderPainted(false);
/* 16 */     button.setOpaque(true);
/* 17 */     button.setCursor(new Cursor(12));
/* 18 */     button.setBorder(BorderFactory.createCompoundBorder(
/* 19 */           BorderFactory.createLineBorder(darkenColor(backgroundColor, 0.8F), 1), 
/* 20 */           BorderFactory.createEmptyBorder(10, 15, 10, 15)));
/*    */ 
/*    */     
/* 23 */     button.addMouseListener(new MouseAdapter() {
/*    */           public void mouseEntered(MouseEvent evt) {
/* 25 */             button.setBackground(UIComponentFactory.darkenColor(backgroundColor, 0.9F));
/*    */           }
/*    */           public void mouseExited(MouseEvent evt) {
/* 28 */             button.setBackground(backgroundColor);
/*    */           }
/*    */         });
/*    */     
/* 32 */     return button;
/*    */   }
/*    */   
/*    */   public static JPanel createTitledPanel(String title, Color borderColor, Component... components) {
/* 36 */     JPanel panel = new JPanel(new FlowLayout(0, 15, 10));
/* 37 */     panel.setBorder(new TitledBorder(new LineBorder(borderColor, 2), title, 0, 0, new Font("Segoe UI", 1, 12), borderColor));
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */     
/* 45 */     panel.setBackground(Color.WHITE);
/*    */     
/* 47 */     for (Component comp : components) {
/* 48 */       panel.add(comp);
/*    */     }
/*    */     
/* 51 */     return panel;
/*    */   }
/*    */   
/*    */   public static JTextField createStyledTextField(int columns) {
/* 55 */     JTextField textField = new JTextField(columns);
/* 56 */     textField.setFont(new Font("Segoe UI", 0, 12));
/* 57 */     textField.setBorder(BorderFactory.createCompoundBorder(
/* 58 */           BorderFactory.createLineBorder(new Color(200, 200, 200)), 
/* 59 */           BorderFactory.createEmptyBorder(5, 8, 5, 8)));
/*    */     
/* 61 */     return textField;
/*    */   }
/*    */   
/*    */   private static Color darkenColor(Color color, float factor) {
/* 65 */     int r = Math.max((int)(color.getRed() * factor), 0);
/* 66 */     int g = Math.max((int)(color.getGreen() * factor), 0);
/* 67 */     int b = Math.max((int)(color.getBlue() * factor), 0);
/* 68 */     return new Color(r, g, b);
/*    */   }
/*    */ }


