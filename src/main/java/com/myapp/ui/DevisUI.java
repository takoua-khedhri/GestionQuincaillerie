package com.myapp.ui;

import com.myapp.db.DatabaseManager;
import com.myapp.logic.DevisManager;
import com.myapp.logic.DevisManager.CalculTotaux;
import com.myapp.logic.ScanService; 
import com.myapp.print.DevisImpression;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.DefaultCellEditor;
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
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

public class DevisUI extends JFrame {

    // --- Composants UI ---
    private JComboBox<String> comboClients;
    private JComboBox<String> comboArticles;
    
    // Boutons pour le mode de prix
    private JRadioButton radioPrixGros;
    private JRadioButton radioPrixDetail;
    private ButtonGroup groupModePrix;
    private boolean modePrixGros = true;
    
    // Champs Article
    private JTextField txtScanCode; 
    private JTextField txtPrix;
    private JTextField txtQuantite;
    
    // Champs Devis
    private JTextField txtDateDevis;
    private JTable tableDetails;
    private DefaultTableModel model;
    
    // Totaux
    private JLabel lblTotalHT;
    private JLabel lblTVA;
    private JLabel lblTotalTTC;
    private JLabel lblNumeroDevis;
    private JLabel lblMontantRemise;
    
    // Gestion PASSAGER / CLIENT BDD
    private JPanel panelClientInfosDB;
    private JTextField txtAdresse;
    private JTextField txtMatricule;
    
    private JPanel panelPassagerInfos;
    private JTextField txtPassagerNom;
    private JTextField txtPassagerPrenom;
    private JTextField txtPassagerTel;
    
    // Managers & Services
    private DevisManager devisManager;
    private ScanService scanService; 
    private DecimalFormat df;
    private ImageIcon logoIcon;
    private JScrollPane mainScrollPane;
    
    // Variables d'état
    private boolean miseAJourEnCours = false;
    
    // Liste cache pour recherche dynamique
    private List<String> cacheArticlesComplet = new ArrayList<>();
    private boolean isAdjusting = false; 

    public DevisUI() {
        df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRANCE));
        
        System.out.println("🚀 Initialisation de DevisUI");
        this.setupManagers();
        this.initializeUI();
        this.loadData();
        this.adjustComponentsForScreen();

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                if (txtScanCode != null) {
                    txtScanCode.requestFocusInWindow();
                }
            }
        });
    }
    
    private void initializeUI() {
        this.setTitle("Système de Devis - CHAA_ELECT");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        this.chargerLogo();
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(this.createHeaderPanel(), BorderLayout.NORTH);
        contentPanel.add(this.createMainPanel(), BorderLayout.CENTER);
        contentPanel.add(this.createTablePanel(), BorderLayout.SOUTH);
        
        mainContainer.add(contentPanel, BorderLayout.CENTER);
        
        this.mainScrollPane = new JScrollPane(mainContainer);
        this.mainScrollPane.setBorder(null);
        this.mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        this.mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        this.getContentPane().add(this.mainScrollPane, BorderLayout.CENTER);
        this.setMinimumSize(new Dimension(1200, 700));
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void setupManagers() {
        // Colonnes : Code, Désignation, Qté, PU HT, Total HT, TVA, PU TTC, Remise %, Total TTC
        String[] columns = {"Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"};
        
        this.model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Colonnes modifiables : Qté (2) et PU TTC (6)
                return column == 2 || column == 6;
            }
        };

        this.model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE && !DevisUI.this.miseAJourEnCours) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    if (column == 2) {
                        SwingUtilities.invokeLater(() -> DevisUI.this.mettreAJourQuantite(row));
                    } else if (column == 6) {
                        SwingUtilities.invokeLater(() -> DevisUI.this.mettreAJourDepuisPUTTC(row));
                    }
                }
            }
        });
        
        this.devisManager = new DevisManager(this.model);
        this.devisManager.setModePrixGros(true);
        this.scanService = new ScanService(); 
    }

    private String formaterPourcentage(double valeur) {
        if (valeur == (int) valeur) {
            return (int) valeur + " %";
        } else {
            return df.format(valeur) + " %";
        }
    }

    private void loadData() {
        this.chargerClients();
        this.chargerArticles();
        this.genererNumeroDevis();
        
        this.comboClients.setSelectedIndex(-1);
        this.comboArticles.setSelectedIndex(-1);
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.4D; gbc.weighty = 1.0D;
        mainPanel.add(this.createClientPanel(), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6D;
        mainPanel.add(this.createArticlesPanel(), gbc);
        return mainPanel;
    }

    private JPanel createClientPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2), 
            "INFORMATIONS CLIENT", 
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14), 
            new Color(52, 152, 219)));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(this.createLabel("Client * :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.7;
        this.comboClients = this.createStyledComboBox();
        this.comboClients.addActionListener((e) -> this.gererAffichageClientPassager());
        formPanel.add(this.comboClients, gbc);

        this.panelClientInfosDB = new JPanel(new GridBagLayout());
        this.panelClientInfosDB.setBackground(Color.WHITE);
        GridBagConstraints gbcDB = new GridBagConstraints();
        gbcDB.insets = new Insets(5, 0, 5, 0); 
        gbcDB.fill = GridBagConstraints.HORIZONTAL; 
        gbcDB.anchor = GridBagConstraints.WEST;

        gbcDB.gridx = 0; gbcDB.gridy = 0; gbcDB.weightx = 0.3;
        this.panelClientInfosDB.add(this.createLabel("Adresse :", Font.BOLD, 12, new Color(60, 60, 60)), gbcDB);
        gbcDB.gridx = 1; gbcDB.weightx = 0.7;
        this.txtAdresse = this.createStyledTextField(); 
        this.txtAdresse.setEditable(false); 
        this.txtAdresse.setBackground(new Color(245, 245, 245));
        this.panelClientInfosDB.add(this.txtAdresse, gbcDB);

        gbcDB.gridx = 0; gbcDB.gridy = 1; gbcDB.weightx = 0.3;
        this.panelClientInfosDB.add(this.createLabel("Matricule :", Font.BOLD, 12, new Color(60, 60, 60)), gbcDB);
        gbcDB.gridx = 1; gbcDB.weightx = 0.7;
        this.txtMatricule = this.createStyledTextField(); 
        this.txtMatricule.setEditable(false); 
        this.txtMatricule.setBackground(new Color(245, 245, 245));
        this.panelClientInfosDB.add(this.txtMatricule, gbcDB);

        this.panelPassagerInfos = new JPanel(new GridBagLayout());
        this.panelPassagerInfos.setBackground(new Color(240, 248, 255)); 
        this.panelPassagerInfos.setBorder(BorderFactory.createTitledBorder("Détails Passager"));
        GridBagConstraints gbcPass = new GridBagConstraints();
        gbcPass.insets = new Insets(5, 5, 5, 5); 
        gbcPass.fill = GridBagConstraints.HORIZONTAL;
        
        gbcPass.gridx = 0; gbcPass.gridy = 0;
        this.panelPassagerInfos.add(new JLabel("Nom :"), gbcPass);
        gbcPass.gridx = 1; gbcPass.weightx = 1.0;
        this.txtPassagerNom = this.createStyledTextField();
        this.panelPassagerInfos.add(this.txtPassagerNom, gbcPass);
        
        gbcPass.gridx = 0; gbcPass.gridy = 1; gbcPass.weightx = 0.0;
        this.panelPassagerInfos.add(new JLabel("Prénom :"), gbcPass);
        gbcPass.gridx = 1; 
        this.txtPassagerPrenom = this.createStyledTextField();
        this.panelPassagerInfos.add(this.txtPassagerPrenom, gbcPass);
        
        gbcPass.gridx = 0; gbcPass.gridy = 2;
        this.panelPassagerInfos.add(new JLabel("Tél :"), gbcPass);
        gbcPass.gridx = 1;
        this.txtPassagerTel = this.createStyledTextField();
        this.panelPassagerInfos.add(this.txtPassagerTel, gbcPass);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4; gbc.weightx = 1.0;
        formPanel.add(this.panelClientInfosDB, gbc);
        
        gbc.gridy = 2;
        formPanel.add(this.panelPassagerInfos, gbc);

        gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAddClient = this.createStyledButton("Nouveau Client BDD", new Color(46, 204, 113));
        btnAddClient.setPreferredSize(new Dimension(250, 40));
        btnAddClient.addActionListener((e) -> { 
             new AjouterClient(null).setVisible(true);
             this.dispose(); 
        });
        formPanel.add(btnAddClient, gbc);

        panel.add(Box.createVerticalStrut(10));
        panel.add(formPanel);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void gererAffichageClientPassager() {
        Object item = comboClients.getSelectedItem();
        if (item == null) return;
        if ("Passager".equals(item.toString())) {
            panelClientInfosDB.setVisible(false);
            panelPassagerInfos.setVisible(true);
            effacerInfosClient();
        } else {
            panelClientInfosDB.setVisible(true);
            panelPassagerInfos.setVisible(false);
            chargerInfosClient();
        }
        this.revalidate(); 
        this.repaint();
    }

    private JPanel createArticlesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(155, 89, 182), 2), 
            "GESTION DES ARTICLES", 
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13), 
            new Color(155, 89, 182)));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Panel Mode Prix
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 5; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel panelModePrix = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panelModePrix.setBackground(Color.WHITE);
        panelModePrix.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 1),
            "Mode de prix",
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
            devisManager.setModePrixGros(true);
            afficherPrixArticle();
        });
        
        this.radioPrixDetail.addActionListener((e) -> {
            modePrixGros = false;
            devisManager.setModePrixGros(false);
            afficherPrixArticle();
        });
        
        panelModePrix.add(radioPrixGros);
        panelModePrix.add(radioPrixDetail);
        formPanel.add(panelModePrix, gbc);

        // SCAN
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        JLabel lblScan = this.createLabel("SCAN (F1) :", Font.BOLD, 12, new Color(231, 76, 60));
        formPanel.add(lblScan, gbc);

        gbc.gridx = 1; gbc.gridwidth = 4;
        this.txtScanCode = this.createStyledTextField();
        this.txtScanCode.setBackground(new Color(255, 252, 230)); 
        this.txtScanCode.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(231, 76, 60), 1), 
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        this.txtScanCode.setToolTipText("Scannez le code barre ici");
        
        this.txtScanCode.addActionListener(e -> {
            String code = txtScanCode.getText();
            boolean success = scanService.traiterScan(code, model, devisManager, this);
            
            if (success) {
                calculerEtAfficherTotaux();
                txtScanCode.setText(""); 
                txtScanCode.requestFocusInWindow();
            } else {
                txtScanCode.selectAll();
            }
        });
        formPanel.add(this.txtScanCode, gbc);

        // Recherche Article
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("Recherche Article :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 4;
        this.comboArticles = new JComboBox<>();
        this.comboArticles.setEditable(true); 
        this.comboArticles.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.comboArticles.setPreferredSize(new Dimension(200, 35));
        
        this.comboArticles.addActionListener((e) -> {
            if (!isAdjusting) {
                this.afficherPrixArticle();
            }
        });
        
        formPanel.add(this.comboArticles, gbc);

        // Quantité et Prix
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("Quantité :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        this.txtQuantite = this.createStyledTextField();
        this.txtQuantite.setPreferredSize(new Dimension(100, 35));
        this.txtQuantite.setText("1");
        this.txtQuantite.addActionListener((e) -> this.ajouterArticle());
        formPanel.add(this.txtQuantite, gbc);

        gbc.gridx = 3; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("PU TTC :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        
        gbc.gridx = 4;
        this.txtPrix = this.createStyledTextField();
        this.txtPrix.setPreferredSize(new Dimension(100, 35));
        this.txtPrix.setEditable(false);
        this.txtPrix.setBackground(new Color(245, 245, 245));
        formPanel.add(this.txtPrix, gbc);

        // Bouton Ajouter
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 5; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAjouterArticle = this.createStyledButton("AJOUTER AU DEVIS", new Color(46, 204, 113));
        btnAjouterArticle.setPreferredSize(new Dimension(250, 40));
        btnAjouterArticle.addActionListener((e) -> this.ajouterArticle());
        formPanel.add(btnAjouterArticle, gbc);

        panel.add(Box.createVerticalStrut(10));
        panel.add(formPanel);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(44, 62, 80)); 
        panel.setBorder(new CompoundBorder(new LineBorder(new Color(30, 40, 50), 1), new EmptyBorder(10, 15, 10, 15)));
        panel.add(this.createBackButtonPanel(), BorderLayout.WEST);
        panel.add(this.createCompanyInfoPanel(), BorderLayout.CENTER);
        panel.add(this.createDevisInfoPanel(), BorderLayout.EAST);
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

    private JPanel createDevisInfoPanel() {
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(44, 62, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        rightPanel.add(this.createLabel("DEVIS N°:", Font.BOLD, 11, Color.WHITE), gbc);
        gbc.gridx = 1;
        this.lblNumeroDevis = this.createLabel("", Font.BOLD, 13, new Color(255, 255, 150));
        rightPanel.add(this.lblNumeroDevis, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        rightPanel.add(this.createLabel("DATE:", Font.BOLD, 11, Color.WHITE), gbc);
        gbc.gridx = 1;
        this.txtDateDevis = new JTextField(LocalDate.now().toString());
        this.styleTextField(this.txtDateDevis);
        this.txtDateDevis.setPreferredSize(new Dimension(120, 25));
        rightPanel.add(this.txtDateDevis, gbc);
        
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
        this.tableDetails.setSelectionBackground(new Color(51, 153, 255));
        this.tableDetails.setSelectionForeground(Color.BLACK);
        this.tableDetails.setGridColor(new Color(240, 240, 240));
        this.tableDetails.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // Largeurs des colonnes
        this.tableDetails.getColumnModel().getColumn(0).setPreferredWidth(80);   // Code
        this.tableDetails.getColumnModel().getColumn(1).setPreferredWidth(180);  // Désignation
        this.tableDetails.getColumnModel().getColumn(2).setPreferredWidth(50);   // Qté
        this.tableDetails.getColumnModel().getColumn(3).setPreferredWidth(80);   // PU HT
        this.tableDetails.getColumnModel().getColumn(4).setPreferredWidth(100);  // Total HT
        this.tableDetails.getColumnModel().getColumn(5).setPreferredWidth(50);   // TVA
        this.tableDetails.getColumnModel().getColumn(6).setPreferredWidth(90);   // PU TTC
        this.tableDetails.getColumnModel().getColumn(7).setPreferredWidth(80);   // Remise %
        this.tableDetails.getColumnModel().getColumn(8).setPreferredWidth(100);  // Total TTC
        
        // Éditeur pour la colonne PU TTC (colonne 6)
        TableColumn puTTCColumn = this.tableDetails.getColumnModel().getColumn(6);
        DefaultCellEditor puTTCEditor = new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean stopCellEditing() {
                JTextField field = (JTextField) getComponent();
                String valeur = field.getText().trim();
                try {
                    valeur = valeur.replace(",", ".").replaceAll("[^0-9.-]", "");
                    double nouveauPUTTC = Double.parseDouble(valeur);
                    if (nouveauPUTTC <= 0) throw new NumberFormatException();
                    int row = tableDetails.getEditingRow();
                    if (row >= 0) {
                        field.setText(formatMontantDinar(nouveauPUTTC));
                        mettreAJourDepuisPUTTC(row);
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "PU TTC invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                return super.stopCellEditing();
            }
        };
        puTTCColumn.setCellEditor(puTTCEditor);
        
        // Renderer personnalisé pour garder le texte noir sur sélection
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                if (isSelected) {
                    setForeground(Color.BLACK);
                    setBackground(new Color(51, 153, 255));
                } else {
                    setForeground(Color.BLACK);
                    setBackground(Color.WHITE);
                }
                return this;
            }
        };
        
        // Renderer pour Total TTC (colonne 8) en vert
        DefaultTableCellRenderer ttcRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                if (isSelected) {
                    setForeground(Color.BLACK);
                    setBackground(new Color(51, 153, 255));
                } else {
                    setForeground(new Color(0, 150, 0));
                    setBackground(new Color(240, 255, 240));
                }
                return this;
            }
        };
        
        // Renderer pour Remise % (colonne 7) en orange
        DefaultTableCellRenderer remiseRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                if (isSelected) {
                    setForeground(Color.BLACK);
                    setBackground(new Color(51, 153, 255));
                } else {
                    setForeground(new Color(192, 57, 43));
                    setBackground(Color.WHITE);
                }
                return this;
            }
        };
        
        // Appliquer les renderers
        for (int i = 0; i < this.tableDetails.getColumnCount(); i++) {
            if (i == 8) {
                this.tableDetails.getColumnModel().getColumn(i).setCellRenderer(ttcRenderer);
            } else if (i == 7) {
                this.tableDetails.getColumnModel().getColumn(i).setCellRenderer(remiseRenderer);
            } else {
                this.tableDetails.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
            }
        }
        
        JTableHeader header = this.tableDetails.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                this.setBackground(new Color(52, 73, 94));
                this.setForeground(Color.WHITE);
                this.setFont(new Font("Segoe UI", Font.BOLD, 11));
                this.setHorizontalAlignment(JLabel.CENTER);
                this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return this;
            }
        });
    }

    // =====================================================================
    // MISE À JOUR DEPUIS PU TTC - Calcul automatique de la remise
    // =====================================================================
    private void mettreAJourDepuisPUTTC(int row) {
        this.miseAJourEnCours = true;
        try {
            String designation = model.getValueAt(row, 1).toString();
            String qteStr = model.getValueAt(row, 2).toString().replaceAll("[^0-9]", "");
            int qte = 1;
            try { qte = Math.max(1, Integer.parseInt(qteStr)); } catch (NumberFormatException ignored) {}

            // Nouveau PU TTC saisi par l'utilisateur
            String puTTCStr = model.getValueAt(row, 6).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
            double nouveauPUTTC = Double.parseDouble(puTTCStr);

            // PRIX DE BASE depuis la BDD (prix original sans remise)
            double prixTTCBase = modePrixGros
                ? DatabaseManager.getPrixGrosArticle(designation)
                : DatabaseManager.getPrixDetailArticle(designation);

            String tvaStr = model.getValueAt(row, 5).toString().replaceAll("[^0-9]", "");
            int tva = Integer.parseInt(tvaStr);

            // Calcul du PU HT à partir du nouveau PU TTC
            double puHT = nouveauPUTTC / (1.0 + tva / 100.0);
            
            // Calcul de la remise en pourcentage par rapport au prix de base
            double remisePct = 0.0;
            if (prixTTCBase > 0 && nouveauPUTTC < prixTTCBase) {
                remisePct = ((prixTTCBase - nouveauPUTTC) / prixTTCBase) * 100.0;
                remisePct = Math.round(remisePct * 100.0) / 100.0;
            }
            
            // Calcul des totaux avec la remise
            double totalTTCBrut = prixTTCBase * qte;
            double totalTTCFinal = nouveauPUTTC * qte;
            double totalHTFinal = puHT * qte;

            // Mise à jour des colonnes
            model.setValueAt(formatMontantDinar(puHT), row, 3);           // PU HT
            model.setValueAt(formatMontantDinar(totalHTFinal), row, 4);   // Total HT
            model.setValueAt(formaterPourcentage(remisePct), row, 7);     // Remise %
            model.setValueAt(formatMontantDinar(totalTTCFinal), row, 8);  // Total TTC

            this.calculerEtAfficherTotaux();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du calcul: " + e.getMessage());
        } finally {
            this.miseAJourEnCours = false;
        }
    }

    // =====================================================================
    // MISE À JOUR DEPUIS QUANTITÉ
    // =====================================================================
    private void mettreAJourQuantite(int row) {
        this.miseAJourEnCours = true;
        try {
            String designation = model.getValueAt(row, 1).toString();
            String qteStr = model.getValueAt(row, 2).toString().replaceAll("[^0-9]", "");
            int nouvelleQte = 1;
            try { nouvelleQte = Math.max(1, Integer.parseInt(qteStr)); } catch (NumberFormatException ignored) {}

            // PRIX DE BASE depuis la BDD
            double prixTTCBase = modePrixGros
                ? DatabaseManager.getPrixGrosArticle(designation)
                : DatabaseManager.getPrixDetailArticle(designation);

            // Lire la remise existante (colonne 7)
            String remiseStr = model.getValueAt(row, 7).toString().replace("%", "").replace(",", ".").trim();
            double remiseExistante = 0.0;
            try { remiseExistante = Double.parseDouble(remiseStr); } catch (NumberFormatException ignored) {}

            String tvaStr = model.getValueAt(row, 5).toString().replaceAll("[^0-9]", "");
            int tva = Integer.parseInt(tvaStr);

            // Calcul du PU TTC après remise
            double puTTC = prixTTCBase * (1.0 - remiseExistante / 100.0);
            double puHT = puTTC / (1.0 + tva / 100.0);
            
            double totalTTC = puTTC * nouvelleQte;
            double totalHT = puHT * nouvelleQte;

            model.setValueAt(formatMontantDinar(puHT), row, 3);   // PU HT
            model.setValueAt(formatMontantDinar(totalHT), row, 4); // Total HT
            model.setValueAt(formatMontantDinar(puTTC), row, 6);   // PU TTC
            model.setValueAt(formatMontantDinar(totalTTC), row, 8); // Total TTC

            this.calculerEtAfficherTotaux();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.miseAJourEnCours = false;
        }
    }

    private JPanel createActionButtons() {
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelButtons.setBackground(Color.WHITE);
        
        JButton btnSupprimer = this.createStyledButton("Supprimer ligne", new Color(231, 76, 60));
        btnSupprimer.addActionListener((e) -> this.supprimerLigne());
        
        JButton btnViderDevis = this.createStyledButton("Nouveau Devis", new Color(155, 89, 182));
        btnViderDevis.addActionListener((e) -> this.viderDevis());
        
        JButton btnGenererDevis = this.createStyledButton("Valider et Imprimer", new Color(46, 204, 113));
        btnGenererDevis.addActionListener((e) -> this.genererDevis());
        
        panelButtons.add(btnSupprimer);
        panelButtons.add(btnViderDevis);
        panelButtons.add(btnGenererDevis);
        return panelButtons;
    }

    private JPanel createTotalPanel() {
        JPanel panelTotal = new JPanel(new BorderLayout(10, 5));
        panelTotal.setBackground(Color.WHITE);
        panelTotal.setBorder(new CompoundBorder(new MatteBorder(2, 0, 0, 0, new Color(52, 152, 219)), new EmptyBorder(10, 0, 5, 0)));
        
        JPanel panelTotaux = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelTotaux.setBackground(Color.WHITE);
        
        panelTotaux.add(this.createLabel("TOTAL REMISE:", Font.BOLD, 12, new Color(44, 62, 80)));
        this.lblMontantRemise = this.createLabel("0,000 DT", Font.BOLD, 12, new Color(192, 57, 43));
        panelTotaux.add(this.lblMontantRemise);
        
        panelTotaux.add(Box.createHorizontalStrut(20));
        panelTotaux.add(this.createLabel("TOTAL HT:", Font.BOLD, 13, new Color(44, 62, 80)));
        this.lblTotalHT = this.createLabel("0,000", Font.BOLD, 14, new Color(52, 152, 219));
        panelTotaux.add(this.lblTotalHT);
        
        panelTotaux.add(Box.createHorizontalStrut(20));
        panelTotaux.add(this.createLabel("TVA:", Font.BOLD, 13, new Color(44, 62, 80)));
        this.lblTVA = this.createLabel("0,000", Font.BOLD, 14, new Color(230, 126, 34));
        panelTotaux.add(this.lblTVA);
        
        panelTotaux.add(Box.createHorizontalStrut(20));
        panelTotaux.add(this.createLabel("TOTAL TTC:", Font.BOLD, 14, new Color(44, 62, 80)));
        this.lblTotalTTC = this.createLabel("0,000", Font.BOLD, 16, new Color(46, 204, 113));
        panelTotaux.add(this.lblTotalTTC);
        
        panelTotal.add(panelTotaux, BorderLayout.CENTER);
        return panelTotal;
    }

    private void chargerClients() {
        this.comboClients.removeAllItems();
        this.comboClients.addItem("Passager");
        DatabaseManager.chargerClients().forEach(this.comboClients::addItem);
        this.comboClients.setSelectedIndex(-1);
    }

    private void chargerArticles() {
        this.cacheArticlesComplet = DatabaseManager.chargerArticles();
        this.comboArticles.removeAllItems();
        for (String article : cacheArticlesComplet) {
            this.comboArticles.addItem(article);
        }
        this.comboArticles.setSelectedIndex(-1);
        this.setupArticleSearch();
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

    private void genererNumeroDevis() {
        String numero = "DEV-" + LocalDate.now().getYear() + "-" + String.format("%04d", (int)(Math.random() * 10000.0D));
        this.lblNumeroDevis.setText(numero);
    }

    private void chargerInfosClient() {
        if (this.comboClients.getSelectedItem() != null) {
            String nomClient = this.comboClients.getSelectedItem().toString();
            String[] infosClient = DatabaseManager.getInfosClientComplet(nomClient);
            this.txtAdresse.setText(infosClient[2]);
            this.txtMatricule.setText(infosClient[4]);
        } else {
            this.effacerInfosClient();
        }
    }

    private void effacerInfosClient() {
        this.txtAdresse.setText("");
        this.txtMatricule.setText("");
    }

    private void afficherPrixArticle() {
        Object item = this.comboArticles.getSelectedItem();
        if (item != null) {
            String article = (String) item;
            double prixTTC;
            
            if (modePrixGros) {
                prixTTC = DatabaseManager.getPrixGrosArticle(article);
            } else {
                prixTTC = DatabaseManager.getPrixDetailArticle(article);
            }
            
            this.txtPrix.setText(formatMontantDinar(prixTTC));
        }
    }

    private void ajouterArticle() {
        try {
            String article = (String)this.comboArticles.getSelectedItem();
            if (article == null || article.isEmpty()) return;
            int qte = Integer.parseInt(this.txtQuantite.getText().trim());
            
            double prixTTC;
            if (modePrixGros) {
                prixTTC = DatabaseManager.getPrixGrosArticle(article);
            } else {
                prixTTC = DatabaseManager.getPrixDetailArticle(article);
            }
            
            this.devisManager.ajouterArticle(article, qte, prixTTC, 0.0);
            this.calculerEtAfficherTotaux();
            this.txtQuantite.setText("1");
            this.txtQuantite.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void supprimerLigne() {
        int selectedRow = this.tableDetails.getSelectedRow();
        if (selectedRow != -1) {
            this.model.removeRow(selectedRow);
            this.calculerEtAfficherTotaux();
        } else {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une ligne à supprimer");
        }
    }

    private void calculerEtAfficherTotaux() {
        CalculTotaux totaux = this.devisManager.calculerTotaux();
        this.lblTotalHT.setText(formatMontantDinar(totaux.totalHT));
        this.lblTVA.setText(formatMontantDinar(totaux.totalTVA));
        this.lblTotalTTC.setText(formatMontantDinar(totaux.totalTTC));
        this.lblMontantRemise.setText(formatMontantDinar(totaux.montantRemise) + " DT");
    }

    private void genererDevis() {
        if (this.model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Ajoutez au moins un article");
            return;
        }
        
        String clientNomSelectionne = (String) this.comboClients.getSelectedItem();
        String nomClientFinal = "";
        String printNom = "", printCode = "", printAdresse = "", printTel = "", printMatricule = "";

        if ("Passager".equals(clientNomSelectionne)) {
            String nom = txtPassagerNom.getText().trim();
            String prenom = txtPassagerPrenom.getText().trim();
            
            if (nom.isEmpty() && prenom.isEmpty()) {
                nomClientFinal = "Passager";
                printNom = "Passager";
            } else {
                nomClientFinal = nom + (prenom.isEmpty() ? "" : " " + prenom);
                printNom = nomClientFinal;
            }
            printTel = txtPassagerTel.getText().trim();
        } else {
            if (clientNomSelectionne == null) {
                JOptionPane.showMessageDialog(this, "Veuillez sélectionner un client");
                return;
            }
            nomClientFinal = clientNomSelectionne;
            String[] infosDB = DatabaseManager.getInfosClientComplet(clientNomSelectionne);
            printNom = infosDB[0]; 
            printCode = infosDB[1]; 
            printAdresse = infosDB[2];
            printTel = infosDB[3]; 
            printMatricule = infosDB[4];
        }

        try {
            if (!this.devisManager.validerDevis(this.lblNumeroDevis.getText(), this.txtDateDevis.getText(), nomClientFinal)) return;

            CalculTotaux totaux = this.devisManager.calculerTotaux();
            
            DevisImpression impression = new DevisImpression(
                this.lblNumeroDevis.getText(), this.txtDateDevis.getText(),
                printNom, printCode, printAdresse, printTel, printMatricule, 
                this.model, "0", this.logoIcon,
                totaux.totalHT, totaux.totalTTC, totaux.montantRemise
            );
            impression.imprimer();
            JOptionPane.showMessageDialog(this, "✅ Devis imprimé avec succès !");
            this.viderDevis();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Erreur impression: " + e.getMessage());
        } catch (Exception e) { 
            e.printStackTrace(); 
            JOptionPane.showMessageDialog(this, "Erreur: " + e.getMessage());
        }
    }

    private void viderDevis() {
        this.model.setRowCount(0);
        this.lblTotalHT.setText("0,000");
        this.lblTVA.setText("0,000");
        this.lblTotalTTC.setText("0,000");
        this.lblMontantRemise.setText("0,000 DT");
        this.genererNumeroDevis();
        this.comboClients.setSelectedIndex(-1);
        this.comboArticles.setSelectedIndex(-1);
        this.txtPrix.setText("");
        this.txtQuantite.setText("1");
        this.txtDateDevis.setText(LocalDate.now().toString());
        this.txtPassagerNom.setText("");
        this.txtPassagerPrenom.setText("");
        this.txtPassagerTel.setText("");
        this.effacerInfosClient();
        if (this.txtScanCode != null) {
            this.txtScanCode.setText("");
            this.txtScanCode.requestFocusInWindow();
        }
    }

    private void chargerLogo() {
        try {
            File f = new File("images/logo.png");
            if (f.exists()) {
                this.logoIcon = new ImageIcon(new ImageIcon(f.getAbsolutePath()).getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH));
            }
        } catch (Exception e) {}
    }

    private String formatMontantDinar(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');  
        DecimalFormat df3 = new DecimalFormat("#,##0.000", symbols);
        String formatted = df3.format(montant).replace(",", ".");
        if (formatted.endsWith(".000")) return formatted.replace(".", ",");
        return formatted;
    }

    private JTextField createStyledTextField() {
        JTextField t = new JTextField();
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), 
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return t;
    }

    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> c = new JComboBox<>();
        c.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 120, 140)), 
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        return c;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setOpaque(true); b.setContentAreaFilled(true); b.setBorderPainted(false);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(darkenColor(bg, 0.9f)); }
            public void mouseExited(MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    private Color darkenColor(Color c, float f) {
        return new Color(Math.max((int)(c.getRed()*f),0), Math.max((int)(c.getGreen()*f),0), Math.max((int)(c.getBlue()*f),0));
    }

    private JLabel createLabel(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }

    private void styleTextField(JTextField t) {
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setBackground(new Color(60, 80, 100));
        t.setForeground(Color.WHITE);
        t.setEditable(false);
    }

    private JPanel createBackButtonPanel() {
        JButton b = createStyledButton("←", new Color(52, 152, 219));
        b.setPreferredSize(new Dimension(40, 30));
        b.addActionListener(e -> {
            new AdminDashboard().setVisible(true); 
            this.dispose(); 
        });
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBackground(new Color(44, 62, 80));
        p.add(b);
        return p;
    }

    private void adjustComponentsForScreen() {
        if (Toolkit.getDefaultToolkit().getScreenSize().width < 1366) {
            this.tableDetails.getColumnModel().getColumn(1).setPreferredWidth(150);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DevisUI().setVisible(true));
    }
}