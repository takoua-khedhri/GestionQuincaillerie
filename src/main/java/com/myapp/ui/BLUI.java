package com.myapp.ui;

import com.myapp.db.DatabaseManager;
import com.myapp.logic.BLManager;
import com.myapp.logic.ScanService;
import com.myapp.print.BLImpression;
import com.myapp.util.AppTheme;
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
import javax.swing.DefaultCellEditor;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BLUI extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(BLUI.class);

    // Composants principaux
    private JComboBox<String> comboClients;
    private JComboBox<String> comboChauffeurs;
    private JComboBox<String> comboVoitures;
    private JComboBox<String> comboArticles;
    private JComboBox<String> comboModePaiement;
    
    // Boutons pour le mode de prix
    private JRadioButton radioPrixGros;
    private JRadioButton radioPrixDetail;
    private ButtonGroup groupModePrix;
    private boolean modePrixGros = true;
    
    // Champs Article
    private JTextField txtScanCode;
    private JTextField txtPrix;
    private JTextField txtQuantite;
    
    // Champs BL
    private JTextField txtDateBL;
    private JTable tableDetails;
    private DefaultTableModel model;
    
    // Labels Totaux
    private JLabel lblTotalHT;
    private JLabel lblTVA;
    private JLabel lblTotalTTC;
    private JLabel lblNumeroBL;
    private JLabel lblMontantRemise;
    
    // Champs Infos Client (BDD) - CLIENT DESTINATAIRE
    private JTextField txtClientAdresse;
    private JTextField txtClientMatricule;
    private JPanel panelClientInfosDB;

    // Champs Infos Chauffeur (BDD) - CHAUFFEUR EXPÉDITEUR
    private JTextField txtChauffeurAdresse;
    private JTextField txtChauffeurTel;
    private JPanel panelChauffeurInfosDB;
    
    // Caches pour recherche dynamique
    private List<String> cacheArticlesComplet = new ArrayList<>();
    private List<String> cacheVoituresComplet = new ArrayList<>();
    private boolean isAdjusting = false; 

    // Managers
    private BLManager blManager;
    private ScanService scanService;
    private DecimalFormat df;
    private ImageIcon logoIcon;
    
    // Variables d'état
    private boolean miseAJourEnCours = false;
    private JScrollPane mainScrollPane;

    public BLUI() {
        df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRANCE));
        log.info("Initialisation de BLUI");
        this.setupManagers();
        this.initializeUI();
        this.loadData();
        this.adjustComponentsForScreen();

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                if (txtScanCode != null) txtScanCode.requestFocusInWindow();
            }
        });
    }

    private void setupManagers() {
        // Colonnes : Code, Désignation, Qté, PU HT, Total HT, TVA, PU TTC, Remise %, Total TTC
        String[] columnNames = {"Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"};
        
        this.model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Colonnes modifiables : Qté (2) et PU TTC (6)
                return column == 2 || column == 6;
            }
        };
        
        this.model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE && !BLUI.this.miseAJourEnCours) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    if (column == 2) {
                        BLUI.this.mettreAJourQuantite(row);
                    } else if (column == 6) {
                        BLUI.this.mettreAJourDepuisPUTTC(row);
                    }
                }
            }
        });
        
        this.blManager = new BLManager(this.model);
        this.blManager.setModePrixGros(true);
        this.scanService = new ScanService();
    }

    private void loadData() {
        this.chargerClients();
        this.chargerChauffeurs();
        this.chargerVoitures();
        this.chargerArticles();
        this.chargerModesPaiement();
        this.genererNumeroBL();
        
        this.comboClients.setSelectedIndex(-1);
        this.comboChauffeurs.setSelectedIndex(-1);
        this.comboVoitures.setSelectedItem("");
        this.comboArticles.setSelectedIndex(-1);
        
        this.effacerInfosClient();
        this.effacerInfosChauffeur();
        
        this.setupVoitureSearch();
    }

    private void viderBL() {
        this.model.setRowCount(0);
        
        this.calculerTotauxGlobaux();
        this.genererNumeroBL();
        
        this.comboClients.setSelectedIndex(-1);
        this.comboChauffeurs.setSelectedIndex(-1);
        this.comboVoitures.setSelectedItem("");
        this.comboArticles.setSelectedIndex(-1);
        this.comboModePaiement.setSelectedItem("Espèces");
        
        this.txtPrix.setText("");
        this.txtQuantite.setText("1");
        this.txtDateBL.setText(LocalDate.now().toString());
        
        this.effacerInfosClient();
        this.effacerInfosChauffeur();
        
        this.txtScanCode.setText("");
        this.txtScanCode.requestFocusInWindow();
    }

    private void initializeUI() {
        this.setTitle("Système de Bon de Livraison - CHAA_ELECT");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        this.chargerLogo();
        AppTheme.init();
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

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5D; gbc.weighty = 1.0D;
        mainPanel.add(this.createChauffeurPanel(), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5D;
        mainPanel.add(this.createClientPanel(), gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0D; gbc.weighty = 1.5D;
        mainPanel.add(this.createArticlesPanel(), gbc);
        return mainPanel;
    }

    private JPanel createChauffeurPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(230, 126, 34), 2), 
            "CHAUFFEUR (EXPÉDITEUR) - Optionnel", 
            javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, 
            new Font("Segoe UI", Font.BOLD, 14), new Color(230, 126, 34)
        ));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0.3D;
        formPanel.add(this.createLabel("Chauffeur :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 0.7D;
        this.comboChauffeurs = new JComboBox<>();
        this.comboChauffeurs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.comboChauffeurs.addActionListener((e) -> this.chargerInfosChauffeur());
        formPanel.add(this.comboChauffeurs, gbc);
        
        this.panelChauffeurInfosDB = new JPanel(new GridBagLayout());
        this.panelChauffeurInfosDB.setBackground(new Color(255, 245, 235));
        GridBagConstraints gbcChauf = new GridBagConstraints();
        gbcChauf.insets = new Insets(5, 5, 5, 5);
        gbcChauf.fill = GridBagConstraints.HORIZONTAL;
        gbcChauf.anchor = GridBagConstraints.WEST;
        
        gbcChauf.gridx = 0; gbcChauf.gridy = 0; gbcChauf.weightx = 0.3;
        this.panelChauffeurInfosDB.add(this.createLabel("Adresse :", Font.BOLD, 12, new Color(60, 60, 60)), gbcChauf);
        gbcChauf.gridx = 1; gbcChauf.weightx = 0.7;
        this.txtChauffeurAdresse = this.createStyledTextField();
        this.txtChauffeurAdresse.setEditable(false);
        this.txtChauffeurAdresse.setBackground(new Color(255, 235, 215));
        this.panelChauffeurInfosDB.add(this.txtChauffeurAdresse, gbcChauf);
        
        gbcChauf.gridx = 0; gbcChauf.gridy = 1; gbcChauf.weightx = 0.3;
        this.panelChauffeurInfosDB.add(this.createLabel("Téléphone :", Font.BOLD, 12, new Color(60, 60, 60)), gbcChauf);
        gbcChauf.gridx = 1; gbcChauf.weightx = 0.7;
        this.txtChauffeurTel = this.createStyledTextField();
        this.txtChauffeurTel.setEditable(false);
        this.txtChauffeurTel.setBackground(new Color(255, 235, 215));
        this.panelChauffeurInfosDB.add(this.txtChauffeurTel, gbcChauf);
        
        JPanel voiturePanel = new JPanel(new GridBagLayout());
        voiturePanel.setBackground(new Color(255, 245, 235));
        voiturePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(230, 126, 34), 1),
                "VÉHICULE",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(230, 126, 34)
        ));
        
        GridBagConstraints gbcVoit = new GridBagConstraints();
        gbcVoit.insets = new Insets(5, 5, 5, 5);
        gbcVoit.fill = GridBagConstraints.HORIZONTAL;
        
        gbcVoit.gridx = 0; gbcVoit.gridy = 0; gbcVoit.weightx = 0.3;
        voiturePanel.add(this.createLabel("Matricule :", Font.BOLD, 12, new Color(60, 60, 60)), gbcVoit);
        gbcVoit.gridx = 1; gbcVoit.weightx = 0.7;
        this.comboVoitures = new JComboBox<>();
        this.comboVoitures.setEditable(true);
        this.comboVoitures.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.comboVoitures.setPreferredSize(new Dimension(200, 35));
        voiturePanel.add(this.comboVoitures, gbcVoit);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        formPanel.add(this.panelChauffeurInfosDB, gbc);
        
        gbc.gridy = 2;
        formPanel.add(voiturePanel, gbc);
        
        panel.add(Box.createVerticalStrut(10));
        panel.add(formPanel);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }

    private JPanel createClientPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2), 
            "CLIENT (DESTINATAIRE) - Optionnel", 
            javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, 
            new Font("Segoe UI", Font.BOLD, 14), new Color(52, 152, 219)
        ));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0.3D;
        formPanel.add(this.createLabel("Client :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 0.7D;
        this.comboClients = new JComboBox<>();
        this.comboClients.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.comboClients.addActionListener((e) -> this.chargerInfosClient());
        formPanel.add(this.comboClients, gbc);
        
        this.panelClientInfosDB = new JPanel(new GridBagLayout());
        this.panelClientInfosDB.setBackground(new Color(235, 245, 255));
        GridBagConstraints gbcClient = new GridBagConstraints();
        gbcClient.insets = new Insets(5, 5, 5, 5);
        gbcClient.fill = GridBagConstraints.HORIZONTAL;
        gbcClient.anchor = GridBagConstraints.WEST;
        
        gbcClient.gridx = 0; gbcClient.gridy = 0; gbcClient.weightx = 0.3;
        this.panelClientInfosDB.add(this.createLabel("Adresse :", Font.BOLD, 12, new Color(60, 60, 60)), gbcClient);
        gbcClient.gridx = 1; gbcClient.weightx = 0.7;
        this.txtClientAdresse = this.createStyledTextField();
        this.txtClientAdresse.setEditable(false);
        this.txtClientAdresse.setBackground(new Color(220, 240, 255));
        this.panelClientInfosDB.add(this.txtClientAdresse, gbcClient);
        
        gbcClient.gridx = 0; gbcClient.gridy = 1; gbcClient.weightx = 0.3;
        this.panelClientInfosDB.add(this.createLabel("Matricule :", Font.BOLD, 12, new Color(60, 60, 60)), gbcClient);
        gbcClient.gridx = 1; gbcClient.weightx = 0.7;
        this.txtClientMatricule = this.createStyledTextField();
        this.txtClientMatricule.setEditable(false);
        this.txtClientMatricule.setBackground(new Color(220, 240, 255));
        this.panelClientInfosDB.add(this.txtClientMatricule, gbcClient);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        formPanel.add(this.panelClientInfosDB, gbc);
        
        gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAddClient = this.createStyledButton("Nouveau Client", new Color(46, 204, 113));
        btnAddClient.setPreferredSize(new Dimension(200, 35));
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

    private JPanel createArticlesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(155, 89, 182), 2), 
            "GESTION DES ARTICLES", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, 
            new Font("Segoe UI", Font.BOLD, 14), new Color(155, 89, 182)
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
            blManager.setModePrixGros(true);
            afficherPrixArticle();
        });
        
        this.radioPrixDetail.addActionListener((e) -> {
            modePrixGros = false;
            blManager.setModePrixGros(false);
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
        this.txtScanCode.setPreferredSize(new Dimension(300, 40));
        this.txtScanCode.addActionListener(e -> {
            String code = txtScanCode.getText();
            boolean success = scanService.traiterScan(code, model, blManager, this);
            if (success) {
                calculerTotauxGlobaux();
                txtScanCode.setText("");
                txtScanCode.requestFocusInWindow();
            } else { txtScanCode.selectAll(); }
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
            if (!isAdjusting) this.afficherPrixArticle();
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
        JButton btnAjouterArticle = this.createStyledButton("AJOUTER AU BL", new Color(46, 204, 113));
        btnAjouterArticle.setPreferredSize(new Dimension(250, 45));
        btnAjouterArticle.setFont(new Font("Segoe UI", Font.BOLD, 14));
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
        panel.setBorder(new CompoundBorder(
            new LineBorder(new Color(30, 40, 50), 1), new EmptyBorder(10, 15, 10, 15)
        ));
        panel.add(this.createBackButtonPanel(), BorderLayout.WEST);
        panel.add(this.createCompanyInfoPanel(), BorderLayout.CENTER);
        panel.add(this.createBLInfoPanel(), BorderLayout.EAST);
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
        infoPanel.add(this.createLabel("Tél:  94 226 752", Font.PLAIN, 11, new Color(200, 220, 255)));
        infoPanel.add(this.createLabel("MF:  000/M/A/1981916C", Font.PLAIN, 11, new Color(200, 220, 255)));
        
        leftPanel.add(infoPanel);
        return leftPanel;
    }

    private JPanel createBLInfoPanel() {
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(44, 62, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        rightPanel.add(this.createLabel("BON DE LIVRAISON N°:", Font.BOLD, 11, Color.WHITE), gbc);
        gbc.gridx = 1;
        this.lblNumeroBL = this.createLabel("", Font.BOLD, 13, new Color(255, 255, 150));
        rightPanel.add(this.lblNumeroBL, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        rightPanel.add(this.createLabel("DATE:", Font.BOLD, 11, Color.WHITE), gbc);
        gbc.gridx = 1;
        this.txtDateBL = new JTextField(LocalDate.now().toString());
        this.styleTextField(this.txtDateBL);
        this.txtDateBL.setPreferredSize(new Dimension(120, 25));
        rightPanel.add(this.txtDateBL, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        rightPanel.add(this.createLabel("MODE:", Font.BOLD, 11, Color.WHITE), gbc);
        gbc.gridx = 1;
        this.comboModePaiement = this.createStyledComboBox();
        this.comboModePaiement.setPreferredSize(new Dimension(120, 25));
        rightPanel.add(this.comboModePaiement, gbc);
        
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

            this.calculerTotauxGlobaux();

        } catch (Exception e) {
            log.error("Erreur lors de la mise a jour depuis PU TTC", e);
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

            this.calculerTotauxGlobaux();

        } catch (Exception e) {
            log.error("Erreur lors de la mise a jour de la quantite", e);
        } finally {
            this.miseAJourEnCours = false;
        }
    }

    private JPanel createActionButtons() {
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelButtons.setBackground(Color.WHITE);
        
        JButton btnSupprimer = this.createStyledButton("Supprimer ligne", new Color(231, 76, 60));
        btnSupprimer.addActionListener((e) -> this.supprimerLigne());
        
        JButton btnViderBL = this.createStyledButton("Nouveau BL", new Color(155, 89, 182));
        btnViderBL.addActionListener((e) -> this.viderBL());
        
        JButton btnGenererBL = this.createStyledButton("Valider et Imprimer", new Color(46, 204, 113));
        btnGenererBL.addActionListener((e) -> this.genererBL());
        
        panelButtons.add(btnSupprimer);
        panelButtons.add(btnViderBL);
        panelButtons.add(btnGenererBL);
        return panelButtons;
    }

    private JPanel createTotalPanel() {
        JPanel panelTotal = new JPanel(new BorderLayout(10, 5));
        panelTotal.setBackground(Color.WHITE);
        panelTotal.setBorder(new CompoundBorder(
            new MatteBorder(2, 0, 0, 0, new Color(52, 152, 219)),
            new EmptyBorder(10, 0, 5, 0)
        ));

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

    private void chargerChauffeurs() {
        this.comboChauffeurs.removeAllItems();
        List<String> clients = DatabaseManager.chargerClients();
        for (String client : clients) {
            this.comboChauffeurs.addItem(client);
        }
        this.comboChauffeurs.setSelectedIndex(-1);
    }

    private void chargerInfosChauffeur() {
        if (this.comboChauffeurs.getSelectedItem() != null) {
            String nomChauffeur = this.comboChauffeurs.getSelectedItem().toString();
            String[] infos = DatabaseManager.getInfosClientComplet(nomChauffeur);
            this.txtChauffeurAdresse.setText(infos[2]);
            this.txtChauffeurTel.setText(infos[3]);
        }
    }

    private void effacerInfosChauffeur() {
        this.txtChauffeurAdresse.setText("");
        this.txtChauffeurTel.setText("");
    }

    private void chargerVoitures() {
        this.cacheVoituresComplet = DatabaseManager.chargerToutesVoitures();
        this.comboVoitures.removeAllItems();
        this.comboVoitures.addItem("");
        for (String voiture : cacheVoituresComplet) {
            this.comboVoitures.addItem(voiture);
        }
        this.comboVoitures.setSelectedItem("");
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

    private void chargerClients() {
        this.comboClients.removeAllItems();
        List<String> clients = DatabaseManager.chargerClients();
        for (String client : clients) {
            this.comboClients.addItem(client);
        }
        this.comboClients.setSelectedIndex(-1);
    }

    private void chargerInfosClient() {
        if (this.comboClients.getSelectedItem() != null) {
            String nomClient = this.comboClients.getSelectedItem().toString();
            String[] infosClient = DatabaseManager.getInfosClientComplet(nomClient);
            this.txtClientAdresse.setText(infosClient[2]);
            this.txtClientMatricule.setText(infosClient[4]);
        }
    }

    private void effacerInfosClient() {
        this.txtClientAdresse.setText("");
        this.txtClientMatricule.setText("");
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
                    for (String item : filtered) newModel.addElement(item);
                    comboArticles.setModel(newModel);
                    editor.setText(text);
                    if (newModel.getSize() > 0) comboArticles.showPopup();
                    else comboArticles.hidePopup();
                    isAdjusting = false;
                });
            }
        });
    }

    private void ajouterArticle() {
        try {
            String article = (String) this.comboArticles.getSelectedItem();
            if (article == null || article.isEmpty()) { 
                JOptionPane.showMessageDialog(this, "Veuillez sélectionner un article"); 
                return; 
            }

            String quantiteText = this.txtQuantite.getText().trim();
            if (quantiteText.isEmpty()) { 
                JOptionPane.showMessageDialog(this, "Veuillez entrer une quantité"); 
                return; 
            }

            int qte = Integer.parseInt(quantiteText);
            
            double prixTTC;
            if (modePrixGros) {
                prixTTC = DatabaseManager.getPrixGrosArticle(article);
            } else {
                prixTTC = DatabaseManager.getPrixDetailArticle(article);
            }
            
            this.blManager.ajouterArticle(article, qte, prixTTC, 0.0);
            this.calculerTotauxGlobaux();
            this.txtQuantite.setText("1");
            this.txtQuantite.requestFocus();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantité invalide (entrez un nombre entier)");
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout d'un article", e);
        }
    }

    private void supprimerLigne() {
        int selectedRow = this.tableDetails.getSelectedRow();
        if (selectedRow != -1) {
            this.model.removeRow(selectedRow);
            this.calculerTotauxGlobaux();
        } else {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une ligne à supprimer");
        }
    }

    private void chargerModesPaiement() {
        this.comboModePaiement.removeAllItems();
        this.comboModePaiement.addItem("Espèces");
        this.comboModePaiement.addItem("Echeance");
        this.comboModePaiement.addItem("Chèque");
        this.comboModePaiement.addItem("Virement");
        this.comboModePaiement.setSelectedItem("Espèces");
    }

  private void mettreAJourStockApresBL() {
        String numeroBL = this.lblNumeroBL.getText();
 
        List<String[]> lignes = new ArrayList<>();
        for (int i = 0; i < this.model.getRowCount(); i++) {
            try {
                String designation = this.model.getValueAt(i, 1).toString();
                String qteStr      = this.model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "");
                int quantite = qteStr.isEmpty() ? 0 : Integer.parseInt(qteStr);
 
                // 1. Mise à jour du stock
                DatabaseManager.mettreAJourStock(designation, quantite);
 
                // 2. Préparer pour le batch historique
                lignes.add(new String[]{designation, String.valueOf(quantite)});
 
            } catch (Exception e) {
                log.error("Erreur mise a jour stock BL ligne {}: {}", i, e.getMessage(), e);
            }
        }
 
        // 3. Enregistrement en batch dans Historique_Sorties
        DatabaseManager.enregistrerSortiesStockBatch(lignes, numeroBL);
    }

    private void genererBL() {
        if (this.model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Ajoutez au moins un article au Bon de Livraison");
            return;
        } 
        
        // Rendre le chauffeur optionnel
        String chauffeurSelectionne = (String) this.comboChauffeurs.getSelectedItem();
        String[] infosChauffeur = null;
        String nomChauffeur = "", prenomChauffeur = "", adresseChauffeur = "", telChauffeur = "";
        
        if (chauffeurSelectionne != null && !chauffeurSelectionne.isEmpty()) {
            infosChauffeur = DatabaseManager.getInfosClientComplet(chauffeurSelectionne);
            nomChauffeur = infosChauffeur[0];
            prenomChauffeur = infosChauffeur[1];
            adresseChauffeur = infosChauffeur[2];
            telChauffeur = infosChauffeur[3];
        }
        
        String voitureSelectionnee = (String) this.comboVoitures.getSelectedItem();
        if (voitureSelectionnee == null) voitureSelectionnee = "";
        
        // Rendre le client destinataire optionnel
        String clientNomSelectionne = (String) this.comboClients.getSelectedItem();
        String printNomClient = "", printPrenomClient = "", printAdresseClient = "", 
               printTelClient = "", printMatriculeClient = "";
        Integer clientId = null; // Valeur par défaut pour client non sélectionné
        String nomClientFinal = "";
        
        if (clientNomSelectionne != null && !clientNomSelectionne.isEmpty()) {
            String[] infosClient = DatabaseManager.getInfosClientComplet(clientNomSelectionne);
            printNomClient = infosClient[0];
            printPrenomClient = infosClient[1];
            printAdresseClient = infosClient[2];
            printTelClient = infosClient[3];
            printMatriculeClient = infosClient[4];
            clientId = DatabaseManager.getClientId(clientNomSelectionne);
            nomClientFinal = printNomClient + " " + printPrenomClient;
        } else {
            // Client par défaut ou anonyme
            nomClientFinal = "Client anonyme";
            printNomClient = "Anonyme";
            printPrenomClient = "";
            printAdresseClient = "Non spécifiée";
            printTelClient = "Non spécifié";
            printMatriculeClient = "Non spécifié";
        }
        
        if (this.comboModePaiement.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un mode de paiement");
            return;
        }

        try {
            String modePaiement = this.comboModePaiement.getSelectedItem().toString();
            
            // Valider le BL sans client obligatoire
            if (!this.blManager.validerBL(this.lblNumeroBL.getText(),
                                           this.txtDateBL.getText(), nomClientFinal)) {
                return;
            }

            BLManager.CalculTotaux totaux = this.blManager.calculerTotaux();
            String remiseTextPourImpression = formatMontantDinar(totaux.montantRemise);

            BLImpression blImpression = new BLImpression(
                this.lblNumeroBL.getText(),
                this.txtDateBL.getText(),
                printNomClient,
                printPrenomClient,
                printAdresseClient,
                printTelClient,
                printMatriculeClient,
                (nomChauffeur + " " + prenomChauffeur).trim(),
                adresseChauffeur,
                telChauffeur,
                voitureSelectionnee,
                this.model,
                remiseTextPourImpression,
                this.logoIcon,
                modePaiement
            );
            
            blImpression.imprimer();

            // ── ENREGISTREMENT EN BASE ──────────────────────────────────────────
            int blId = DatabaseManager.insererFactureAvecRetourId(
                this.lblNumeroBL.getText(),   // numero
                "BL",                          // type
                this.txtDateBL.getText(),      // date
                clientId,                      // client_id (peut être null)
                nomClientFinal,                // client_nom
                totaux.totalHT,                // montant_ht
                totaux.totalTVA,               // tva
                totaux.totalTTC,               // montant_ttc
                0.0,                           // remise_pourcentage
                totaux.montantRemise,          // montant_remise
                modePaiement,                  // moyen_paiement
                0.0,                           // timbre
                printTelClient,                // tel_client
                0.0,                           // retenue_source
                totaux.totalTTC                // net_a_payer
            );

            if (blId > 0) {
                for (int i = 0; i < this.model.getRowCount(); i++) {
                    String designation = this.model.getValueAt(i, 1).toString();
                    String qteStr = this.model.getValueAt(i, 2).toString()
                                        .replaceAll("[^0-9]", "");
                    int qte = Integer.parseInt(qteStr);
                    String puHTStr = this.model.getValueAt(i, 3).toString()
                                        .replace(",", ".").replaceAll("[^0-9.-]", "");
                    double puHT = Double.parseDouble(puHTStr);
                    String tvaStr = this.model.getValueAt(i, 5).toString()
                                        .replaceAll("[^0-9]", "");
                    int tva = tvaStr.isEmpty() ? 0 : Integer.parseInt(tvaStr);
                    DatabaseManager.insererDetailFacture(blId, designation, qte, puHT, tva);
                }
                log.info("BL enregistre en base, ID = {}", blId);
            } else {
                log.error("Echec enregistrement BL en base");
                JOptionPane.showMessageDialog(this,
                    "⚠️ BL imprimé mais non enregistré en base !",
                    "Avertissement", JOptionPane.WARNING_MESSAGE);
            }
            // ───────────────────────────────────────────────────────────────────

            this.mettreAJourStockApresBL();
            
            JOptionPane.showMessageDialog(this,
                "✅ Bon de Livraison enregistré et stock mis à jour !");
            this.viderBL();

        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de l'impression : " + ex.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la generation du BL", e);
            JOptionPane.showMessageDialog(this,
                "Erreur inattendue : " + e.getMessage());
        }
    }

    private void calculerTotauxGlobaux() {
        BLManager.CalculTotaux totaux = this.blManager.calculerTotaux();
        this.lblTotalHT.setText(formatMontantDinar(totaux.totalHT));
        this.lblTVA.setText(formatMontantDinar(totaux.totalTVA));
        this.lblMontantRemise.setText(formatMontantDinar(totaux.montantRemise) + " DT");
        this.lblTotalTTC.setText(formatMontantDinar(totaux.totalTTC));
    }

    private String formaterPourcentage(double valeur) {
        if (valeur == (int) valeur) {
            return (int) valeur + " %";
        } else {
            return df.format(valeur) + " %";
        }
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

    private void chargerLogo() {
        try {
            String[] paths = {"src/images/logo.png", "images/logo.png"};
            for(String p : paths) { 
                File f = new File(p); 
                if(f.exists()) { 
                    this.logoIcon = new ImageIcon(new ImageIcon(f.getAbsolutePath()).getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH)); 
                    break; 
                } 
            }
        } catch(Exception e) { log.error("Erreur lors du chargement du logo", e); }
    }


    private void genererNumeroBL() {
        int year = LocalDate.now().getYear();
        String numero = "BL-" + year + "-" + String.format("%04d", (int) (Math.random() * 10000.0D));
        this.lblNumeroBL.setText(numero);
    }

    private JLabel createLabel(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
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
            public void mouseEntered(MouseEvent evt) { button.setBackground(BLUI.this.darkenColor(backgroundColor, 0.9F)); }
            public void mouseExited(MouseEvent evt) { button.setBackground(backgroundColor); }
        });
        return button;
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

    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboBox.setBackground(Color.WHITE);
        return comboBox;
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

    private void adjustComponentsForScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (screenSize.getWidth() < 1366.0D) {
            this.tableDetails.getColumnModel().getColumn(1).setPreferredWidth(150); 
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BLUI().setVisible(true));
    }
}