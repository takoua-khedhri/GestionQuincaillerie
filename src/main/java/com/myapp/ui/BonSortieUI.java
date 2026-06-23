package com.myapp.ui;

import com.myapp.db.DatabaseManager;
import com.myapp.logic.BonSortieManager;
import com.myapp.logic.BonSortieManager.CalculTotaux;
import com.myapp.logic.ScanService; 
import com.myapp.print.BonSortieImpression;
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
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

public class BonSortieUI extends JFrame {

    // Composants principaux
    private JComboBox<String> comboClients;
    private JComboBox<String> comboArticles;
    private JComboBox<String> comboVoitures;
    
    // Champs Article
    private JTextField txtScanCode; 
    private JTextField txtPrix;
    private JTextField txtQuantite;
    
    // Champs Bon de Sortie
    private JTextField txtDateBonSortie;
    private JTextArea txtObservations;
    
    // Table et Modèle
    private JTable tableDetails;
    private DefaultTableModel model;
    
    // Labels Totaux
    private JLabel lblTotalHT;
    private JLabel lblTotalTTC;
    private JLabel lblNumeroBonSortie;
    private JLabel lblMontantRemise;
    
    // GESTION PASSAGER / CLIENT BDD
    private JPanel panelClientInfosDB;
    private JTextField txtAdresse;
    private JTextField txtMatricule;
    
    private JPanel panelPassagerInfos;
    private JTextField txtPassagerNom;
    private JTextField txtPassagerPrenom;
    private JTextField txtPassagerTel;
    
    // Panneau pour les informations de voiture
    private JPanel panelVoitureInfos;
    
    // Boutons pour le mode de prix
    private JRadioButton radioPrixGros;
    private JRadioButton radioPrixDetail;
    private ButtonGroup groupModePrix;
    private boolean modePrixGros = true;
    
    // Managers
    private BonSortieManager bonSortieManager;
    private ScanService scanService; 
    private DecimalFormat df;
    private ImageIcon logoIcon;
    private boolean miseAJourEnCours = false;
    
    // CACHE POUR RECHERCHE DYNAMIQUE
    private List<String> cacheArticlesComplet = new ArrayList<>();
    private List<String> cacheVoituresComplet = new ArrayList<>();
    private boolean isAdjusting = false; 

    public BonSortieUI() {
        df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRANCE));
        
        System.out.println("🚀 Initialisation de BonSortieUI");
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
        this.setTitle("Système de Bon de Sortie - CHAA_ELECT");
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
                if (e.getType() == TableModelEvent.UPDATE && !BonSortieUI.this.miseAJourEnCours) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    if (column == 2) {
                        SwingUtilities.invokeLater(() -> BonSortieUI.this.mettreAJourQuantite(row));
                    } else if (column == 6) {
                        SwingUtilities.invokeLater(() -> BonSortieUI.this.mettreAJourDepuisPUTTC(row));
                    }
                }
            }
        });
        
        this.bonSortieManager = new BonSortieManager(this.model);
        this.bonSortieManager.setModePrixGros(true);
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
        this.chargerVoitures();
        this.genererNumeroBonSortie();
        
        this.comboClients.setSelectedIndex(-1);
        this.comboArticles.setSelectedIndex(-1);
        this.comboVoitures.setSelectedItem("");
        
        this.setupVoitureSearch();
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

        this.panelVoitureInfos = new JPanel(new GridBagLayout());
        this.panelVoitureInfos.setBackground(new Color(255, 245, 235));
        this.panelVoitureInfos.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(230, 126, 34), 1),
                "VÉHICULE",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(230, 126, 34)
        ));
        
        GridBagConstraints gbcVoiture = new GridBagConstraints();
        gbcVoiture.insets = new Insets(5, 5, 5, 5);
        gbcVoiture.fill = GridBagConstraints.HORIZONTAL;

        gbcVoiture.gridx = 0; gbcVoiture.gridy = 0; gbcVoiture.weightx = 0.3;
        JLabel lblMatriculeVoiture = this.createLabel("Matricule :", Font.BOLD, 12, new Color(60, 60, 60));
        this.panelVoitureInfos.add(lblMatriculeVoiture, gbcVoiture);

        gbcVoiture.gridx = 1; gbcVoiture.weightx = 0.7;
        this.comboVoitures = new JComboBox<>();
        this.comboVoitures.setEditable(true);
        this.comboVoitures.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.comboVoitures.setPreferredSize(new Dimension(200, 35));
        this.panelVoitureInfos.add(this.comboVoitures, gbcVoiture);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4; gbc.weightx = 1.0;
        formPanel.add(this.panelClientInfosDB, gbc);
        
        gbc.gridy = 2;
        formPanel.add(this.panelPassagerInfos, gbc);
        
        gbc.gridy = 3;
        formPanel.add(this.panelVoitureInfos, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
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
                new Font("Segoe UI", Font.BOLD, 14), 
                new Color(155, 89, 182)
        ));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
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
            bonSortieManager.setModePrixGros(true);
            afficherPrixArticle();
        });
        
        this.radioPrixDetail.addActionListener((e) -> {
            modePrixGros = false;
            bonSortieManager.setModePrixGros(false);
            afficherPrixArticle();
        });
        
        panelModePrix.add(radioPrixGros);
        panelModePrix.add(radioPrixDetail);
        formPanel.add(panelModePrix, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        JLabel lblScan = this.createLabel("SCAN (F1) :", Font.BOLD, 13, new Color(231, 76, 60));
        formPanel.add(lblScan, gbc);

        gbc.gridx = 1; gbc.gridwidth = 3;
        this.txtScanCode = this.createStyledTextField();
        this.txtScanCode.setBackground(new Color(255, 252, 230)); 
        this.txtScanCode.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        this.txtScanCode.setToolTipText("Scannez le code barre ici");
        this.txtScanCode.setPreferredSize(new Dimension(300, 40));
        
        this.txtScanCode.addActionListener(e -> {
            String code = txtScanCode.getText();
            boolean success = scanService.traiterScan(code, model, bonSortieManager, this);
            
            if (success) {
                calculerEtAfficherTotaux(); 
                txtScanCode.setText(""); 
                txtScanCode.requestFocusInWindow();
            } else {
                txtScanCode.selectAll();
            }
        });
        formPanel.add(this.txtScanCode, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("Recherche Article :", Font.BOLD, 13, new Color(60, 60, 60)), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3;
        this.comboArticles = new JComboBox<>();
        this.comboArticles.setEditable(true);
        this.comboArticles.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.comboArticles.setPreferredSize(new Dimension(300, 40));
        
        this.comboArticles.addActionListener((e) -> {
            if (!isAdjusting) {
                this.afficherPrixArticle();
            }
        });
        formPanel.add(this.comboArticles, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("Quantité :", Font.BOLD, 13, new Color(60, 60, 60)), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 1;
        this.txtQuantite = this.createStyledTextField();
        this.txtQuantite.setPreferredSize(new Dimension(100, 40));
        this.txtQuantite.setText("1");
        this.txtQuantite.setHorizontalAlignment(JTextField.CENTER);
        this.txtQuantite.addActionListener((e) -> this.ajouterArticle());
        formPanel.add(this.txtQuantite, gbc);

        gbc.gridx = 2; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("PU TTC :", Font.BOLD, 13, new Color(60, 60, 60)), gbc);
        
        gbc.gridx = 3; gbc.gridwidth = 1;
        this.txtPrix = this.createStyledTextField();
        this.txtPrix.setPreferredSize(new Dimension(120, 40));
        this.txtPrix.setEditable(false);
        this.txtPrix.setBackground(new Color(245, 245, 245));
        this.txtPrix.setHorizontalAlignment(JTextField.RIGHT);
        formPanel.add(this.txtPrix, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAdd = this.createStyledButton("AJOUTER AU BON", new Color(46, 204, 113));
        btnAdd.setPreferredSize(new Dimension(250, 45));
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.addActionListener((e) -> this.ajouterArticle());
        formPanel.add(btnAdd, gbc);

        panel.add(Box.createVerticalStrut(10));
        panel.add(formPanel);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(44, 62, 80));
        panel.setBorder(new CompoundBorder(
            new LineBorder(new Color(30, 40, 50), 1), 
            new EmptyBorder(10, 15, 10, 15)
        ));
        panel.add(this.createBackButtonPanel(), BorderLayout.WEST);
        panel.add(this.createCompanyInfoPanel(), BorderLayout.CENTER);
        panel.add(this.createBonSortieInfoPanel(), BorderLayout.EAST);
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
            lblLogo = new JLabel("CHAA_ELCT");
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
        infoPanel.add(this.createLabel("MF:  000/M/A/1981916C", Font.PLAIN, 11, new Color(200, 220, 255)));
        
        leftPanel.add(infoPanel);
        return leftPanel;
    }

    private JPanel createBonSortieInfoPanel() {
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(44, 62, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        rightPanel.add(this.createLabel("BON DE SORTIE N°:", Font.BOLD, 11, Color.WHITE), gbc);
        
        gbc.gridx = 1;
        this.lblNumeroBonSortie = this.createLabel("", Font.BOLD, 13, new Color(255, 255, 150));
        rightPanel.add(this.lblNumeroBonSortie, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        rightPanel.add(this.createLabel("DATE:", Font.BOLD, 11, Color.WHITE), gbc);
        
        gbc.gridx = 1;
        this.txtDateBonSortie = new JTextField(LocalDate.now().toString());
        this.styleTextField(this.txtDateBonSortie);
        this.txtDateBonSortie.setPreferredSize(new Dimension(120, 25));
        rightPanel.add(this.txtDateBonSortie, gbc);
        
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
        this.tableDetails.setRowHeight(35);
        this.tableDetails.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
                this.setFont(new Font("Segoe UI", Font.BOLD, 12));
                this.setHorizontalAlignment(JLabel.CENTER);
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

            int stockActuel = DatabaseManager.getStockActuel(designation);
            if (nouvelleQte > stockActuel) {
                JOptionPane.showMessageDialog(this,
                    "Stock insuffisant pour '" + designation + "' ! Stock: " + stockActuel);
                model.setValueAt("1", row, 2);
                nouvelleQte = 1;
            }

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
        
        JButton btnVider = this.createStyledButton("Nouveau Bon", new Color(155, 89, 182));
        btnVider.addActionListener((e) -> this.viderBonSortie());
        
        JButton btnGenerer = this.createStyledButton("Valider et Imprimer", new Color(46, 204, 113));
        btnGenerer.addActionListener((e) -> this.genererBonSortie());
        
        panelButtons.add(btnSupprimer);
        panelButtons.add(btnVider);
        panelButtons.add(btnGenerer);
        return panelButtons;
    }

    private JPanel createTotalPanel() {
        JPanel panelTotal = new JPanel(new BorderLayout(10, 5));
        panelTotal.setBackground(Color.WHITE);
        panelTotal.setBorder(new CompoundBorder(
                new MatteBorder(2, 0, 0, 0, new Color(52, 152, 219)),
                new EmptyBorder(10, 0, 5, 0)));

        JPanel panelTotaux = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelTotaux.setBackground(Color.WHITE);

        panelTotaux.add(this.createLabel("TOTAL REMISE:", Font.BOLD, 12, new Color(44, 62, 80)));
        this.lblMontantRemise = this.createLabel("0,000 DT", Font.BOLD, 12, new Color(192, 57, 43));
        panelTotaux.add(this.lblMontantRemise);

        panelTotaux.add(Box.createHorizontalStrut(20));
        panelTotaux.add(this.createLabel("TOTAL HT:", Font.BOLD, 14, new Color(44, 62, 80)));
        this.lblTotalHT = this.createLabel("0,000", Font.BOLD, 16, new Color(52, 152, 219));
        panelTotaux.add(this.lblTotalHT);

        panelTotaux.add(Box.createHorizontalStrut(20));
        panelTotaux.add(this.createLabel("TOTAL TTC:", Font.BOLD, 16, new Color(44, 62, 80)));
        this.lblTotalTTC = this.createLabel("0,000", Font.BOLD, 18, new Color(46, 204, 113));
        panelTotaux.add(this.lblTotalTTC);

        panelTotal.add(panelTotaux, BorderLayout.CENTER);
        return panelTotal;
    }

    private JButton createStyledButton(String text, final Color backgroundColor) {
        final JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(this.darkenColor(backgroundColor, 0.8F), 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { button.setBackground(BonSortieUI.this.darkenColor(backgroundColor, 0.9F)); }
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
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
        btnRetour.setPreferredSize(new Dimension(40, 35));
        btnRetour.addActionListener((e) -> {
            new AdminDashboard().setVisible(true);
            this.dispose();
        });
        JPanel retourPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        retourPanel.setBackground(new Color(44, 62, 80));
        retourPanel.add(btnRetour);
        return retourPanel;
    }

    private void chargerInfosClient() {
        if (this.comboClients.getSelectedItem() != null) {
            String nomClient = this.comboClients.getSelectedItem().toString();
            String[] infos = DatabaseManager.getInfosClientComplet(nomClient);
            this.txtAdresse.setText(infos[2]);
            this.txtMatricule.setText(infos[4]);
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

    private void chargerArticles() {
        this.cacheArticlesComplet = DatabaseManager.chargerArticles();
        
        this.comboArticles.removeAllItems();
        for (String article : cacheArticlesComplet) {
            this.comboArticles.addItem(article);
        }
        
        this.setupArticleSearch();
    }
    
    private void chargerVoitures() {
        this.cacheVoituresComplet = DatabaseManager.chargerToutesVoitures();
        
        this.comboVoitures.removeAllItems();
        this.comboVoitures.addItem("");
        for (String voiture : cacheVoituresComplet) {
            this.comboVoitures.addItem(voiture);
        }
        
        this.setupVoitureSearch();
    }
    
    private void setupVoitureSearch() {
        final JTextComponent editor = (JTextComponent) comboVoitures.getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN || 
                    e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_LEFT || 
                    e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    isAdjusting = true; 
                    
                    String text = editor.getText();
                    List<String> filtered = cacheVoituresComplet.stream()
                        .filter(item -> item.toLowerCase().contains(text.toLowerCase()))
                        .collect(Collectors.toList());

                    DefaultComboBoxModel<String> newModel = new DefaultComboBoxModel<>();
                    newModel.addElement("");
                    for (String item : filtered) {
                        newModel.addElement(item);
                    }
                    
                    comboVoitures.setModel(newModel);
                    editor.setText(text);
                    
                    if (newModel.getSize() > 1) {
                        comboVoitures.showPopup();
                    } else {
                        comboVoitures.hidePopup();
                    }
                    
                    isAdjusting = false;
                });
            }
        });
    }

    private void setupArticleSearch() {
        final JTextComponent editor = (JTextComponent) comboArticles.getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN || 
                    e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_LEFT || 
                    e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    isAdjusting = true; 
                    
                    String text = editor.getText();
                    List<String> filtered = cacheArticlesComplet.stream()
                        .filter(item -> item.toLowerCase().contains(text.toLowerCase()))
                        .collect(Collectors.toList());

                    DefaultComboBoxModel<String> newModel = new DefaultComboBoxModel<>();
                    for (String item : filtered) {
                        newModel.addElement(item);
                    }
                    
                    comboArticles.setModel(newModel);
                    editor.setText(text);
                    
                    if (newModel.getSize() > 0) {
                        comboArticles.showPopup();
                    } else {
                        comboArticles.hidePopup();
                    }
                    
                    isAdjusting = false;
                });
            }
        });
    }

    private void ajouterArticle() {
        try {
            String article = (String) this.comboArticles.getSelectedItem();
            if (article == null || article.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Sélectionnez un article");
                return;
            }

            String qteStr = this.txtQuantite.getText().trim();
            if (qteStr.isEmpty()) return;

            int qte = Integer.parseInt(qteStr);
            if (qte <= 0) {
                JOptionPane.showMessageDialog(this, "Quantité positive requise");
                return;
            }
            
            double prixTTC;
            if (modePrixGros) {
                prixTTC = DatabaseManager.getPrixGrosArticle(article);
            } else {
                prixTTC = DatabaseManager.getPrixDetailArticle(article);
            }
            
            this.bonSortieManager.ajouterArticle(article, qte, prixTTC, 0.0);
            this.calculerEtAfficherTotaux();
            this.txtQuantite.setText("1");
            this.txtQuantite.requestFocus();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantité invalide");
        }
    }

    private void supprimerLigne() {
        int selectedRow = this.tableDetails.getSelectedRow();
        if (selectedRow != -1) {
            this.model.removeRow(selectedRow);
            this.calculerEtAfficherTotaux();
        } else {
            JOptionPane.showMessageDialog(this, "Sélectionnez une ligne");
        }
    }

    private void calculerEtAfficherTotaux() {
        CalculTotaux totaux = this.bonSortieManager.calculerTotaux();
        this.lblTotalHT.setText(formatMontantDinar(totaux.totalHT));
        this.lblTotalTTC.setText(formatMontantDinar(totaux.totalTTC));
        this.lblMontantRemise.setText(formatMontantDinar(totaux.montantRemise) + " DT");
    }
    
    private String formatMontantDinar(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df3 = new DecimalFormat("#,##0.000", symbols);
        
        String formatted = df3.format(montant);
        formatted = formatted.replace(",", ".");
        
        if (formatted.endsWith(".000")) {
            return formatted.replace(".", ",");
        }
        
        return formatted;
    }

    private void genererBonSortie() {
        if (this.model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Ajoutez des articles");
            return;
        }
        
        String clientNomSelectionne = (String) this.comboClients.getSelectedItem();
        String nomClientFinal = "";
        
        String printNom = "", printCode = "", printAdresse = "", printTel = "", printMatricule = "";

        if ("Passager".equals(clientNomSelectionne)) {
            String nomPassager = txtPassagerNom.getText().trim();
            String prenomPassager = txtPassagerPrenom.getText().trim();
            
            if (!nomPassager.isEmpty() || !prenomPassager.isEmpty()) {
                nomClientFinal = nomPassager + " " + prenomPassager;
            } else {
                nomClientFinal = "Passager";
            }
            
            printNom = nomClientFinal;
            printTel = txtPassagerTel.getText().trim();
        } else {
            if (clientNomSelectionne == null) {
                JOptionPane.showMessageDialog(this, "Sélectionnez un client");
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

        String voitureSelectionnee = (String) this.comboVoitures.getSelectedItem();
        if (voitureSelectionnee == null) voitureSelectionnee = "";

        try {
            if (!this.bonSortieManager.validerBonSortie(this.lblNumeroBonSortie.getText(), 
                    this.txtDateBonSortie.getText(), nomClientFinal)) {
                return;
            }

            CalculTotaux totaux = this.bonSortieManager.calculerTotaux();

            BonSortieImpression impression = new BonSortieImpression(
                this.lblNumeroBonSortie.getText(),
                this.txtDateBonSortie.getText(),
                printNom,
                printCode,
                printAdresse,
                printTel,
                printMatricule,
                voitureSelectionnee,
                "",
                "",
                this.model,
                formatMontantDinar(totaux.montantRemise),
                this.logoIcon,
                totaux.totalHT,
                totaux.totalTTC,
                totaux.montantRemise
            );
            
            impression.imprimer();
            JOptionPane.showMessageDialog(this, "✅ Bon de Sortie imprimé avec succès !");
            this.viderBonSortie();

        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Erreur impression : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage());
        }
    }

    private void viderBonSortie() {
        this.model.setRowCount(0);
        this.lblTotalHT.setText("0,000");
        this.lblTotalTTC.setText("0,000");
        this.lblMontantRemise.setText("0,000 DT");
        this.genererNumeroBonSortie();
        
        this.comboClients.setSelectedIndex(-1);
        this.comboArticles.setSelectedIndex(-1);
        this.comboVoitures.setSelectedItem("");
        this.txtQuantite.setText("1");
        
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
            String[] paths = {"src/images/logo.png", "images/logo.png"};
            File logoFile = null;
            for (String path : paths) {
                File f = new File(path);
                if (f.exists()) {
                    logoFile = f;
                    break;
                }
            }
            if (logoFile != null) {
                this.logoIcon = new ImageIcon(logoFile.getAbsolutePath());
                Image image = this.logoIcon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                this.logoIcon = new ImageIcon(image);
            }
        } catch (Exception e) {}
    }

    private void applyModernLook() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
    }

    private void chargerClients() {
        this.comboClients.removeAllItems();
        this.comboClients.addItem("Passager");
        DatabaseManager.chargerClients().forEach(this.comboClients::addItem);
        this.comboClients.setSelectedIndex(-1);
    }

    private void genererNumeroBonSortie() {
        String num = "BS-" + LocalDate.now().getYear() + "-" + String.format("%04d", (int)(Math.random() * 10000));
        this.lblNumeroBonSortie.setText(num);
    }

    private void adjustComponentsForScreen() {
        if (Toolkit.getDefaultToolkit().getScreenSize().width < 1366) {
            this.tableDetails.getColumnModel().getColumn(1).setPreferredWidth(150);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BonSortieUI().setVisible(true));
    }
}