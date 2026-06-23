package com.myapp.ui;

import com.myapp.db.DatabaseManager;
import com.myapp.logic.BonCommandeManager;
import com.myapp.logic.BonCommandeManager.CalculTotaux;
import com.myapp.print.BonCommandeImpression;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;

public class BonCommandeUI extends JFrame {

    // Composants principaux
    private JComboBox<String> comboFournisseurs;
    private JComboBox<String> comboArticles;
    
    // Boutons pour le mode de prix
    private JRadioButton radioPrixGros;
    private JRadioButton radioPrixDetail;
    private ButtonGroup groupModePrix;
    private boolean modePrixGros = true;
    
    // Champs Article
    private JTextField txtPrixAchat;
    private JTextField txtQuantite;
    
    // Champs Bon de Commande
    private JTextField txtDateBonCommande;
    private JTextArea txtObservations;
    
    // Table et Modèle
    private JTable tableDetails;
    private DefaultTableModel model;
    
    // Labels Totaux
    private JLabel lblTotalHT;
    private JLabel lblTotalTTC;
    private JLabel lblNumeroBonCommande;
    private JLabel lblMontantRemise;
    
    // GESTION FOURNISSEUR / ACHETEUR BDD
    private JPanel panelFournisseurInfosDB;
    private JTextField txtAdresseFournisseur;
    private JTextField txtMatriculeFournisseur;
    private JTextField txtTelFournisseur;
    
    private JPanel panelAcheteurInfos;
    private JTextField txtAcheteurNom;
    private JTextField txtAcheteurPrenom;
    private JTextField txtAcheteurTel;
    private JTextField txtAcheteurEmail;
    
    // Managers
    private BonCommandeManager bonCommandeManager;
    private DecimalFormat df;
    private DecimalFormat df3;
    private ImageIcon logoIcon;
    private boolean miseAJourEnCours = false;
    
    // CACHE POUR RECHERCHE DYNAMIQUE
    private List<String> cacheArticlesComplet = new ArrayList<>();
    private boolean isAdjusting = false;

    public BonCommandeUI() {
        // Init DecimalFormat avec locale France
        df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRANCE));
        df3 = new DecimalFormat("#,##0.000", new DecimalFormatSymbols(Locale.FRANCE));
        
        System.out.println("🚀 Initialisation de BonCommandeUI");
        this.setupManagers();
        this.initializeUI();
        this.loadData();
        this.adjustComponentsForScreen();
    }

    private void initializeUI() {
        this.setTitle("Système de Bon de Commande - CHAA ELECT");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        this.chargerLogo();
        this.applyModernLook();
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(this.createHeaderPanel(), BorderLayout.NORTH);
        contentPanel.add(this.createMainPanel(), BorderLayout.CENTER);
        contentPanel.add(this.createTablePanel(), BorderLayout.SOUTH);
        
        mainContainer.add(contentPanel, BorderLayout.CENTER);
        
        JScrollPane mainScrollPane = new JScrollPane(mainContainer);
        mainScrollPane.setBorder(null);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        this.getContentPane().add(mainScrollPane, BorderLayout.CENTER);
        this.setMinimumSize(new Dimension(1200, 700));
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void setupManagers() {
        String[] columns = {"Référence", "Désignation", "Quantité", "PU Achat", "Remise (%)", "Total HT"};
        
        this.model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 4;
            }
        };

        this.model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE && !BonCommandeUI.this.miseAJourEnCours) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    if (column == 2 || column == 4) {
                        SwingUtilities.invokeLater(() -> BonCommandeUI.this.mettreAJourLigne(row));
                    }
                }
            }
        });
        
        this.bonCommandeManager = new BonCommandeManager(this.model);
    }

    private String formaterPourcentage(double valeur) {
        if (valeur == (int) valeur) {
            return (int) valeur + " %";
        } else {
            return df.format(valeur) + " %";
        }
    }

    private void mettreAJourLigne(int row) {
        this.miseAJourEnCours = true;
        try {
            Object qteObj = this.model.getValueAt(row, 2);
            if (qteObj == null) return;
            int qte = Integer.parseInt(qteObj.toString());
            if (qte <= 0) throw new NumberFormatException();

            Object remiseObj = this.model.getValueAt(row, 4);
            if (remiseObj == null) return;
            String remiseStr = remiseObj.toString().replace("%", "").replace(",", ".").trim();
            double remisePct = Double.parseDouble(remiseStr);
            this.model.setValueAt(formaterPourcentage(remisePct), row, 4);

            Object puObj = this.model.getValueAt(row, 3);
            if (puObj == null) return;
            double pu = Double.parseDouble(puObj.toString().replace(",", ".").replaceAll("[^0-9.-]", ""));

            double totalBrut = pu * qte;
            double montantRemiseLigne = totalBrut * (remisePct / 100.0);
            double totalHTLigne = totalBrut - montantRemiseLigne;

            this.model.setValueAt(formatMontantDinar(totalHTLigne), row, 5);
            this.calculerEtAfficherTotaux();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.miseAJourEnCours = false;
        }
    }

    private void loadData() {
        this.chargerFournisseurs();
        this.chargerArticles();
        this.genererNumeroBonCommande();
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5D; gbc.weighty = 1.0D;
        mainPanel.add(this.createAcheteurFournisseurPanel(), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5D;
        mainPanel.add(this.createArticlesPanel(), gbc);
        return mainPanel;
    }

    private JPanel createAcheteurFournisseurPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
                "INFORMATIONS FOURNISSEUR",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(52, 152, 219)
        ));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(this.createLabel("Fournisseur * :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.7;
        this.comboFournisseurs = this.createStyledComboBox();
        this.comboFournisseurs.addActionListener((e) -> this.gererAffichageFournisseurAcheteur());
        formPanel.add(this.comboFournisseurs, gbc);

        this.panelFournisseurInfosDB = new JPanel(new GridBagLayout());
        this.panelFournisseurInfosDB.setBackground(Color.WHITE);
        GridBagConstraints gbcDB = new GridBagConstraints();
        gbcDB.insets = new Insets(5, 0, 5, 0);
        gbcDB.fill = GridBagConstraints.HORIZONTAL;
        gbcDB.anchor = GridBagConstraints.WEST;

        gbcDB.gridx = 0; gbcDB.gridy = 0; gbcDB.weightx = 0.3;
        this.panelFournisseurInfosDB.add(this.createLabel("Adresse :", Font.BOLD, 12, new Color(60, 60, 60)), gbcDB);
        gbcDB.gridx = 1; gbcDB.weightx = 0.7;
        this.txtAdresseFournisseur = this.createStyledTextField();
        this.txtAdresseFournisseur.setEditable(false);
        this.txtAdresseFournisseur.setBackground(new Color(245, 245, 245));
        this.panelFournisseurInfosDB.add(this.txtAdresseFournisseur, gbcDB);

        gbcDB.gridx = 0; gbcDB.gridy = 1;
        this.panelFournisseurInfosDB.add(this.createLabel("Matricule :", Font.BOLD, 12, new Color(60, 60, 60)), gbcDB);
        gbcDB.gridx = 1;
        this.txtMatriculeFournisseur = this.createStyledTextField();
        this.txtMatriculeFournisseur.setEditable(false);
        this.txtMatriculeFournisseur.setBackground(new Color(245, 245, 245));
        this.panelFournisseurInfosDB.add(this.txtMatriculeFournisseur, gbcDB);

        gbcDB.gridx = 0; gbcDB.gridy = 2;
        this.panelFournisseurInfosDB.add(this.createLabel("Téléphone :", Font.BOLD, 12, new Color(60, 60, 60)), gbcDB);
        gbcDB.gridx = 1;
        this.txtTelFournisseur = this.createStyledTextField();
        this.txtTelFournisseur.setEditable(false);
        this.txtTelFournisseur.setBackground(new Color(245, 245, 245));
        this.panelFournisseurInfosDB.add(this.txtTelFournisseur, gbcDB);

        this.panelAcheteurInfos = new JPanel(new GridBagLayout());
        this.panelAcheteurInfos.setBackground(new Color(240, 248, 255));
        this.panelAcheteurInfos.setBorder(BorderFactory.createTitledBorder("Détails Acheteur (Nouveau Fournisseur)"));
        GridBagConstraints gbcAch = new GridBagConstraints();
        gbcAch.insets = new Insets(5, 5, 5, 5);
        gbcAch.fill = GridBagConstraints.HORIZONTAL;
        
        gbcAch.gridx = 0; gbcAch.gridy = 0;
        this.panelAcheteurInfos.add(new JLabel("Nom * :"), gbcAch);
        gbcAch.gridx = 1; gbcAch.weightx = 1.0;
        this.txtAcheteurNom = this.createStyledTextField();
        this.panelAcheteurInfos.add(this.txtAcheteurNom, gbcAch);
        
        gbcAch.gridx = 0; gbcAch.gridy = 1;
        this.panelAcheteurInfos.add(new JLabel("Prénom :"), gbcAch);
        gbcAch.gridx = 1;
        this.txtAcheteurPrenom = this.createStyledTextField();
        this.panelAcheteurInfos.add(this.txtAcheteurPrenom, gbcAch);
        
        gbcAch.gridx = 0; gbcAch.gridy = 2;
        this.panelAcheteurInfos.add(new JLabel("Tél :"), gbcAch);
        gbcAch.gridx = 1;
        this.txtAcheteurTel = this.createStyledTextField();
        this.panelAcheteurInfos.add(this.txtAcheteurTel, gbcAch);
        
        gbcAch.gridx = 0; gbcAch.gridy = 3;
        this.panelAcheteurInfos.add(new JLabel("Email :"), gbcAch);
        gbcAch.gridx = 1;
        this.txtAcheteurEmail = this.createStyledTextField();
        this.panelAcheteurInfos.add(this.txtAcheteurEmail, gbcAch);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4; gbc.weightx = 1.0;
        formPanel.add(this.panelFournisseurInfosDB, gbc);
        gbc.gridy = 2;
        formPanel.add(this.panelAcheteurInfos, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("Obs. :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        this.txtObservations = new JTextArea(3, 20);
        this.txtObservations.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.txtObservations.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        this.txtObservations.setLineWrap(true);
        this.txtObservations.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(this.txtObservations), gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAddFournisseur = this.createStyledButton("Nouveau Fournisseur", new Color(46, 204, 113));
        btnAddFournisseur.setPreferredSize(new Dimension(250, 40));
        btnAddFournisseur.addActionListener((e) -> {
             new AjouterFournisseur(BonCommandeUI.this).setVisible(true);
             BonCommandeUI.this.dispose();
        });
        formPanel.add(btnAddFournisseur, gbc);

        panel.add(Box.createVerticalStrut(10));
        panel.add(formPanel);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void gererAffichageFournisseurAcheteur() {
        Object item = comboFournisseurs.getSelectedItem();
        if (item == null) return;
        if ("Nouveau Fournisseur".equals(item.toString())) {
            panelFournisseurInfosDB.setVisible(false);
            panelAcheteurInfos.setVisible(true);
            effacerInfosFournisseur();
        } else {
            panelFournisseurInfosDB.setVisible(true);
            panelAcheteurInfos.setVisible(false);
            chargerInfosFournisseur();
        }
        this.revalidate();
        this.repaint();
    }

    private JPanel createArticlesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(155, 89, 182), 2),
                "GESTION DES ARTICLES À COMMANDER",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                new Color(155, 89, 182)
        ));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Panel pour les boutons radio de sélection du mode de prix
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 5; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel panelModePrix = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panelModePrix.setBackground(Color.WHITE);
        panelModePrix.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 1),
            "Mode de prix d'achat",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 11),
            new Color(52, 152, 219)
        ));
        
        this.radioPrixGros = new JRadioButton("Prix Gros");
        this.radioPrixDetail = new JRadioButton("Prix Détail");
        this.radioPrixGros.setFont(new Font("Segoe UI", Font.BOLD, 12));
        this.radioPrixDetail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        this.radioPrixGros.setSelected(true);
        
        this.groupModePrix = new ButtonGroup();
        this.groupModePrix.add(radioPrixGros);
        this.groupModePrix.add(radioPrixDetail);
        
        this.radioPrixGros.addActionListener((e) -> {
            modePrixGros = true;
            System.out.println("Mode prix: GROS");
            afficherPrixArticle();
        });
        
        this.radioPrixDetail.addActionListener((e) -> {
            modePrixGros = false;
            System.out.println("Mode prix: DÉTAIL");
            afficherPrixArticle();
        });
        
        panelModePrix.add(radioPrixGros);
        panelModePrix.add(radioPrixDetail);
        formPanel.add(panelModePrix, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("Recherche Article :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3;
        this.comboArticles = new JComboBox<>();
        this.comboArticles.setEditable(true);
        this.comboArticles.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.comboArticles.addActionListener((e) -> { if (!isAdjusting) this.afficherPrixArticle(); });
        formPanel.add(this.comboArticles, gbc);

        gbc.gridx = 4; gbc.gridwidth = 1;
        JButton btnSelect = this.createStyledButton("SELECT", new Color(52, 152, 219));
        btnSelect.setPreferredSize(new Dimension(100, 35));
        btnSelect.addActionListener((e) -> this.selectionnerArticlesManquants());
        formPanel.add(btnSelect, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("Quantité * :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        this.txtQuantite = this.createStyledTextField();
        this.txtQuantite.setPreferredSize(new Dimension(100, 35));
        this.txtQuantite.setText("1");
        this.txtQuantite.addActionListener((e) -> this.ajouterArticle());
        formPanel.add(this.txtQuantite, gbc);

        gbc.gridx = 3; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("Prix Achat :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        gbc.gridx = 4; gbc.gridwidth = 1;
        this.txtPrixAchat = this.createStyledTextField();
        this.txtPrixAchat.setPreferredSize(new Dimension(100, 35));
        this.txtPrixAchat.setText("0");
        formPanel.add(this.txtPrixAchat, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 5; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAdd = this.createStyledButton("AJOUTER AU BON DE COMMANDE", new Color(46, 204, 113));
        btnAdd.setPreferredSize(new Dimension(300, 40));
        btnAdd.addActionListener((e) -> this.ajouterArticle());
        formPanel.add(btnAdd, gbc);

        panel.add(Box.createVerticalStrut(10));
        panel.add(formPanel);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void selectionnerArticlesManquants() {
        List<String> articlesManquants = DatabaseManager.getArticlesEnRupture();
        if (articlesManquants.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun article en rupture de stock.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Voulez-vous ajouter les articles en rupture au bon ?", "Articles manquants", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            for (String article : articlesManquants) {
                double prix = 0.0;
                this.bonCommandeManager.ajouterArticle(article, 1, prix);
            }
            this.calculerEtAfficherTotaux();
        }
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(44, 62, 80));
        panel.setBorder(new CompoundBorder(new LineBorder(new Color(30, 40, 50), 1), new EmptyBorder(10, 15, 10, 15)));
        panel.add(this.createBackButtonPanel(), BorderLayout.WEST);
        panel.add(this.createCompanyInfoPanel(), BorderLayout.CENTER);
        panel.add(this.createBonCommandeInfoPanel(), BorderLayout.EAST);
        return panel;
    }

    private JPanel createCompanyInfoPanel() {
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setBackground(new Color(44, 62, 80));
        
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(new Color(44, 62, 80));
        logoPanel.setPreferredSize(new Dimension(60, 60));
        
        JLabel lblLogo;
        if (this.logoIcon != null) {
            lblLogo = new JLabel(this.logoIcon);
        } else {
            lblLogo = new JLabel("CHAA ELECT");
            lblLogo.setFont(new Font("Segoe UI", Font.PLAIN, 24));
            lblLogo.setForeground(Color.WHITE);
        }
        lblLogo.setHorizontalAlignment(JLabel.CENTER);
        logoPanel.add(lblLogo, BorderLayout.CENTER);

        leftPanel.add(logoPanel);
        
        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 0, 2));
        infoPanel.setBackground(new Color(44, 62, 80));
        infoPanel.add(this.createLabel("CHAA ELECT", Font.BOLD, 16, Color.WHITE));
        infoPanel.add(this.createLabel("08, RUE 42500, Ezzahrouni, El Hrairia, Tunis, 2051", Font.PLAIN, 11, new Color(200, 220, 255)));
        infoPanel.add(this.createLabel("Tél: 94 226 752", Font.PLAIN, 11, new Color(200, 220, 255)));
        infoPanel.add(this.createLabel("MF: 000/M/A/1981916C", Font.PLAIN, 11, new Color(200, 220, 255)));
        
        leftPanel.add(infoPanel);
        return leftPanel;
    }

    private JPanel createBonCommandeInfoPanel() {
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(44, 62, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        rightPanel.add(this.createLabel("BON DE COMMANDE N°:", Font.BOLD, 11, Color.WHITE), gbc);
        
        gbc.gridx = 1;
        this.lblNumeroBonCommande = this.createLabel("", Font.BOLD, 13, new Color(255, 255, 150));
        rightPanel.add(this.lblNumeroBonCommande, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        rightPanel.add(this.createLabel("DATE:", Font.BOLD, 11, Color.WHITE), gbc);
        
        gbc.gridx = 1;
        this.txtDateBonCommande = new JTextField(LocalDate.now().toString());
        this.styleTextField(this.txtDateBonCommande);
        this.txtDateBonCommande.setPreferredSize(new Dimension(120, 25));
        rightPanel.add(this.txtDateBonCommande, gbc);
        
        return rightPanel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.WHITE);
        
        this.setupTable();
        
        JScrollPane scrollPane = new JScrollPane(this.tableDetails);
        scrollPane.setPreferredSize(new Dimension(0, 250));
        
        panel.add(this.createActionButtons(), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(this.createTotalPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private void setupTable() {
        this.tableDetails = new JTable(this.model);
        this.tableDetails.setRowHeight(30);
        this.tableDetails.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        this.tableDetails.setSelectionBackground(new Color(220, 240, 255));
        this.tableDetails.setGridColor(new Color(240, 240, 240));
        
        this.tableDetails.getColumnModel().getColumn(0).setPreferredWidth(80);
        this.tableDetails.getColumnModel().getColumn(1).setPreferredWidth(200);
        this.tableDetails.getColumnModel().getColumn(2).setPreferredWidth(60);
        this.tableDetails.getColumnModel().getColumn(3).setPreferredWidth(80);
        this.tableDetails.getColumnModel().getColumn(4).setPreferredWidth(70);
        this.tableDetails.getColumnModel().getColumn(5).setPreferredWidth(90);
        
        DefaultTableCellRenderer editableRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(new Color(255, 255, 224));
                }
                return c;
            }
        };
        this.tableDetails.getColumnModel().getColumn(2).setCellRenderer(editableRenderer);
        this.tableDetails.getColumnModel().getColumn(4).setCellRenderer(editableRenderer);
        
        JTableHeader header = this.tableDetails.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                this.setBackground(new Color(52, 73, 94));
                this.setForeground(Color.WHITE);
                this.setFont(new Font("Segoe UI", Font.BOLD, 11));
                this.setHorizontalAlignment(JLabel.CENTER);
                return this;
            }
        });
    }

    private JPanel createActionButtons() {
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelButtons.setBackground(Color.WHITE);
        
        JButton btnSupprimer = this.createStyledButton("Supprimer ligne", new Color(231, 76, 60));
        btnSupprimer.addActionListener((e) -> this.supprimerLigne());
        
        JButton btnVider = this.createStyledButton("Nouveau Bon", new Color(155, 89, 182));
        btnVider.addActionListener((e) -> this.viderBonCommande());
        
        JButton btnGenerer = this.createStyledButton("Valider et Imprimer", new Color(46, 204, 113));
        btnGenerer.addActionListener((e) -> this.genererBonCommande());
        
        panelButtons.add(btnSupprimer);
        panelButtons.add(btnVider);
        panelButtons.add(btnGenerer);
        return panelButtons;
    }

    /**
     * Panel des totaux SANS remise globale
     * Affiche uniquement : TOTAL REMISE, TOTAL HT, TOTAL TTC
     */
    private JPanel createTotalPanel() {
        JPanel panelTotal = new JPanel(new BorderLayout(10, 5));
        panelTotal.setBackground(Color.WHITE);
        panelTotal.setBorder(new CompoundBorder(
                new MatteBorder(2, 0, 0, 0, new Color(52, 152, 219)),
                new EmptyBorder(10, 0, 5, 0)));

        JPanel panelTotaux = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelTotaux.setBackground(Color.WHITE);
        
        panelTotaux.add(this.createLabel("TOTAL REMISE:", Font.BOLD, 12, new Color(44, 62, 80)));
        this.lblMontantRemise = this.createLabel("0,000", Font.BOLD, 12, new Color(192, 57, 43));
        panelTotaux.add(this.lblMontantRemise);
        
        panelTotaux.add(Box.createHorizontalStrut(20));
        
        panelTotaux.add(this.createLabel("TOTAL HT:", Font.BOLD, 13, new Color(44, 62, 80)));
        this.lblTotalHT = this.createLabel("0,000", Font.BOLD, 14, new Color(52, 152, 219));
        panelTotaux.add(this.lblTotalHT);
        
        panelTotaux.add(Box.createHorizontalStrut(20));
        
        panelTotaux.add(this.createLabel("TOTAL TTC:", Font.BOLD, 14, new Color(44, 62, 80)));
        this.lblTotalTTC = this.createLabel("0,000", Font.BOLD, 16, new Color(46, 204, 113));
        panelTotaux.add(this.lblTotalTTC);

        panelTotal.add(panelTotaux, BorderLayout.CENTER);
        return panelTotal;
    }

    private JButton createStyledButton(String text, final Color backgroundColor) {
        final JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(this.darkenColor(backgroundColor, 0.8F), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { button.setBackground(BonCommandeUI.this.darkenColor(backgroundColor, 0.9F)); }
            public void mouseExited(MouseEvent evt) { button.setBackground(backgroundColor); }
        });
        return button;
    }

    private JTextField createStyledTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        textField.setBackground(Color.WHITE);
        return textField;
    }

    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboBox.setBackground(Color.WHITE);
        return comboBox;
    }

    private JLabel createLabel(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private Color darkenColor(Color color, float factor) {
        return new Color(
            Math.max((int)(color.getRed() * factor), 0),
            Math.max((int)(color.getGreen() * factor), 0),
            Math.max((int)(color.getBlue() * factor), 0)
        );
    }
    
    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textField.setBackground(new Color(60, 80, 100));
        textField.setForeground(Color.WHITE);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 120, 140)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        textField.setEditable(false);
    }

    private JPanel createBackButtonPanel() {
        JButton btnRetour = this.createStyledButton("←", new Color(52, 152, 219));
        btnRetour.setPreferredSize(new Dimension(40, 30));
        btnRetour.addActionListener((e) -> {
            new AdminDashboard().setVisible(true);
            this.dispose();
        });
        JPanel retourPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        retourPanel.setBackground(new Color(44, 62, 80));
        retourPanel.add(btnRetour);
        return retourPanel;
    }

    private void chargerInfosFournisseur() {
        if (this.comboFournisseurs.getSelectedItem() != null) {
            String nomFournisseur = this.comboFournisseurs.getSelectedItem().toString();
            String[] infos = DatabaseManager.getInfosFournisseurComplet(nomFournisseur);
            if (infos.length >= 5) {
                this.txtAdresseFournisseur.setText(infos[2] != null ? infos[2] : "");
                this.txtMatriculeFournisseur.setText(infos[4] != null ? infos[4] : "");
                this.txtTelFournisseur.setText(infos[3] != null ? infos[3] : "");
            }
        }
    }

    private void effacerInfosFournisseur() {
        this.txtAdresseFournisseur.setText("");
        this.txtMatriculeFournisseur.setText("");
        this.txtTelFournisseur.setText("");
    }

    private void afficherPrixArticle() {
        Object item = this.comboArticles.getSelectedItem();
        if (item != null) {
            String article = (String) item;
            double prix;
            
            if (modePrixGros) {
                prix = DatabaseManager.getPrixGrosArticle(article);
                System.out.println("Prix Gros pour " + article + ": " + prix);
            } else {
                prix = DatabaseManager.getPrixDetailArticle(article);
                System.out.println("Prix Détail pour " + article + ": " + prix);
            }
            
            this.txtPrixAchat.setText(formatMontantDinar(prix));
            this.txtPrixAchat.requestFocus();
        }
    }

    private void chargerArticles() {
        this.cacheArticlesComplet = DatabaseManager.chargerTousArticles();
        this.comboArticles.removeAllItems();
        for (String article : cacheArticlesComplet) this.comboArticles.addItem(article);
        this.setupArticleSearch();
    }

    private void chargerFournisseurs() {
        this.comboFournisseurs.removeAllItems();
        this.comboFournisseurs.addItem("Nouveau Fournisseur");
        List<String> fournisseurs = DatabaseManager.chargerFournisseurs();
        for (String fournisseur : fournisseurs) this.comboFournisseurs.addItem(fournisseur);
    }

    private void setupArticleSearch() {
        final JTextComponent editor = (JTextComponent) comboArticles.getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN || 
                    e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_LEFT || 
                    e.getKeyCode() == KeyEvent.VK_RIGHT) return;

                SwingUtilities.invokeLater(() -> {
                    isAdjusting = true;
                    String text = editor.getText();
                    List<String> filtered = cacheArticlesComplet.stream()
                        .filter(item -> item.toLowerCase().contains(text.toLowerCase()))
                        .collect(Collectors.toList());
                    DefaultComboBoxModel<String> newModel = new DefaultComboBoxModel<>();
                    for (String item : filtered) newModel.addElement(item);
                    comboArticles.setModel(newModel);
                    editor.setText(text);
                    if (newModel.getSize() > 0) comboArticles.showPopup(); else comboArticles.hidePopup();
                    isAdjusting = false;
                });
            }
        });
    }

    private void ajouterArticle() {
        try {
            String article = this.comboArticles.getEditor().getItem().toString();
            if (article.isEmpty()) return;
            int qte = Integer.parseInt(this.txtQuantite.getText().trim());
            
            String prixStr = this.txtPrixAchat.getText().trim();
            double prix;
            if (prixStr.isEmpty() || prixStr.equals("0") || prixStr.equals("0,000")) {
                JOptionPane.showMessageDialog(this, 
                    "Veuillez saisir le prix d'achat pour l'article : " + article,
                    "Prix manquant",
                    JOptionPane.WARNING_MESSAGE);
                this.txtPrixAchat.requestFocus();
                return;
            }
            
            prix = Double.parseDouble(prixStr.replace(",", "."));

            System.out.println("Ajout de " + article + " - Quantité: " + qte + " - Prix achat: " + prix + " (Mode: " + (modePrixGros ? "GROS" : "DÉTAIL") + ")");

            this.bonCommandeManager.ajouterArticle(article, qte, prix);
            this.calculerEtAfficherTotaux();
            this.txtQuantite.setText("1");
            this.txtPrixAchat.setText("0");
            this.comboArticles.getEditor().setItem("");
            this.txtQuantite.requestFocus();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Le prix doit être un nombre valide",
                "Erreur de saisie",
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            // Ignorer les autres erreurs
        }
    }

    private void supprimerLigne() {
        int selectedRow = this.tableDetails.getSelectedRow();
        if (selectedRow != -1) {
            this.model.removeRow(selectedRow);
            this.calculerEtAfficherTotaux();
        }
    }

    /**
     * Calcule et affiche les totaux
     * Version MODIFIÉE : sans remise globale
     */
    private void calculerEtAfficherTotaux() {
        CalculTotaux totaux = this.bonCommandeManager.calculerTotaux();
        this.lblTotalHT.setText(formatMontantDinar(totaux.totalHT));
        this.lblTotalTTC.setText(formatMontantDinar(totaux.totalTTC));
        this.lblMontantRemise.setText(formatMontantDinar(totaux.montantRemise));
    }
    
    private String formatMontantDinar(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df3 = new DecimalFormat("#,##0.000", symbols);
        String formatted = df3.format(montant).replace(",", ".");
        if (formatted.endsWith(".000")) return formatted.replace(".", ",");
        return formatted;
    }

    /**
     * Génère et imprime le Bon de Commande
     * Version MODIFIÉE : sans remise globale
     */
    private void genererBonCommande() {
        if (this.model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, 
                "Veuillez ajouter au moins un article avant de générer le bon de commande.",
                "Aucun article",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Object selectedFournisseur = this.comboFournisseurs.getSelectedItem();
        
        if (selectedFournisseur == null || selectedFournisseur.toString().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "❌ Veuillez sélectionner un fournisseur avant de générer le bon de commande.\n\n" +
                "Pour cela, vous pouvez :\n" +
                "• Choisir un fournisseur existant dans la liste déroulante\n" +
                "• Cliquer sur le bouton 'Nouveau Fournisseur' pour en créer un",
                "Fournisseur non sélectionné",
                JOptionPane.ERROR_MESSAGE);
            this.comboFournisseurs.requestFocus();
            return;
        }
        
        String fournisseurSelectionne = selectedFournisseur.toString();
        String nomFournisseurFinal = "";
        String pNomF = "", pAdrF = "", pTelF = "", pMatF = "", pNomA = "", pPreA = "", pTelA = "", pEmaA = "";

        if ("Nouveau Fournisseur".equals(fournisseurSelectionne)) {
            if (txtAcheteurNom.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Veuillez saisir le nom de fournisseur.",
                    "Informations manquantes",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            nomFournisseurFinal = txtAcheteurNom.getText().trim() + " " + txtAcheteurPrenom.getText().trim();
            pNomA = txtAcheteurNom.getText().trim(); pPreA = txtAcheteurPrenom.getText().trim();
            pTelA = txtAcheteurTel.getText().trim(); pEmaA = txtAcheteurEmail.getText().trim();
        } else {
            nomFournisseurFinal = fournisseurSelectionne;
            String[] infosDB = DatabaseManager.getInfosFournisseurComplet(fournisseurSelectionne);
            if (infosDB != null && infosDB.length >= 5) {
                pNomF = infosDB[0] != null ? infosDB[0] : ""; 
                pAdrF = infosDB[2] != null ? infosDB[2] : ""; 
                pTelF = infosDB[3] != null ? infosDB[3] : ""; 
                pMatF = infosDB[4] != null ? infosDB[4] : "";
            }
        }

        try {
            if (!this.bonCommandeManager.validerBonCommande(this.lblNumeroBonCommande.getText(), this.txtDateBonCommande.getText(), nomFournisseurFinal)) return;

            // Calculer les totaux sans remise globale
            CalculTotaux totaux = this.bonCommandeManager.calculerTotaux();
            
            BonCommandeImpression impression = new BonCommandeImpression(
                this.lblNumeroBonCommande.getText(), this.txtDateBonCommande.getText(),
                pNomF, pAdrF, pTelF, pMatF, pNomA, pPreA, pTelA, pEmaA,
                this.txtObservations.getText(), this.model, "0", this.logoIcon,  // remise globale à 0
                totaux.totalHT, totaux.totalTTC, totaux.montantRemise
            );
            impression.imprimer();
            this.viderBonCommande();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Vide le Bon de Commande
     * Version MODIFIÉE : sans réinitialisation de txtRemise
     */
    private void viderBonCommande() {
        this.model.setRowCount(0);
        this.lblTotalHT.setText("0,000"); 
        this.lblTotalTTC.setText("0,000"); 
        this.lblMontantRemise.setText("0,000");
        this.genererNumeroBonCommande();
        this.comboFournisseurs.setSelectedIndex(-1); 
        this.comboArticles.setSelectedIndex(-1);
        this.txtQuantite.setText("1"); 
        this.txtPrixAchat.setText("0"); 
        this.txtObservations.setText("");
        this.txtAcheteurNom.setText(""); 
        this.txtAcheteurPrenom.setText(""); 
        this.txtAcheteurTel.setText(""); 
        this.txtAcheteurEmail.setText("");
        this.effacerInfosFournisseur();
        this.panelFournisseurInfosDB.setVisible(true); 
        this.panelAcheteurInfos.setVisible(false);
    }

    private void chargerLogo() {
        try {
            File f = new File("images/logo.png");
            if (f.exists()) {
                this.logoIcon = new ImageIcon(new ImageIcon(f.getAbsolutePath()).getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH));
            }
        } catch (Exception e) {}
    }

    private void applyModernLook() { try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {} }

    private void genererNumeroBonCommande() {
        String num = "BC-" + LocalDate.now().getYear() + "-" + String.format("%04d", (int)(Math.random() * 10000));
        this.lblNumeroBonCommande.setText(num);
    }

    private void adjustComponentsForScreen() {
        if (Toolkit.getDefaultToolkit().getScreenSize().width < 1366) {
            this.tableDetails.getColumnModel().getColumn(1).setPreferredWidth(150);
        }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new BonCommandeUI().setVisible(true)); }
}