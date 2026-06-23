package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import com.myapp.db.DatabaseManager;
import com.myapp.logic.FactureManager;
import com.myapp.logic.FactureManager.CalculTotaux;
import com.myapp.logic.ScanService;
import com.myapp.print.FactureImpression;
import com.myapp.print.FactureImpressionTicket;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
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

public class FactureUI extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(FactureUI.class);

    // --- COMPOSANTS UI ---
    private JComboBox<String> comboClients;
    private JComboBox<String> comboArticles;
    private JComboBox<String> comboModePaiement;
    private JComboBox<String> comboVoitures;

    private JRadioButton radioPrixGros;
    private JRadioButton radioPrixDetail;
    private ButtonGroup groupModePrix;
    private boolean modePrixGros = true;

    // Champs Article
    private JTextField txtScanCode;
    private JTextField txtPrix;
    private JTextField txtQuantite;

    // Champs Facture
    private JTextField txtDateFacture;
    private JTable tableDetails;
    private DefaultTableModel model;

    // Labels Totaux
    private JLabel lblTotalHT;
    private JLabel lblTVA;
    private JLabel lblTotalTTC;
    private JLabel lblNumeroFacture;
    private JLabel lblMontantRemise;

    // Retenue à la source
    private JCheckBox chkRetenueSource;
    private JComboBox<String> comboTauxRetenue;
    private JLabel lblMontantRetenue;
    private JLabel lblNetAPayer;

    private static final double[] TAUX_RETENUE = {1.0};
    private static final String[] TAUX_RETENUE_LABELS = {
        "1 % (Ventes marchandises)"
    };
    private static final double SEUIL_RETENUE_HT = 1000.0;

    // Champs Infos Client
    private JTextField txtAdresse;
    private JTextField txtMatricule;
    private JPanel panelClientInfosDB;
    private JPanel panelPassagerInfos;
    private JTextField txtPassagerNom;
    private JTextField txtPassagerPrenom;
    private JTextField txtPassagerTel;
    private JPanel panelVoitureInfos;

    // Caches
    private List<String> cacheArticlesComplet = new ArrayList<>();
    private List<String> cacheVoituresComplet = new ArrayList<>();
    private boolean isAdjusting = false;

    // Managers
    private FactureManager factureManager;
    private ScanService scanService;
    private DecimalFormat df;
    private ImageIcon logoIcon;

    // Variables d'état
    private int factureIdEnCours = -1;
    private boolean modeModification = false;
    private boolean miseAJourEnCours = false;
    private JScrollPane mainScrollPane;
    private volatile boolean factureEnCours = false;
    private JButton btnGenererFacture;
    private JSpinner spinnerTimbre;

    public FactureUI() {
        df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRANCE));
        log.info("Initialisation de FactureUI");
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
        String[] columnNames = {"Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"};

        this.model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 6;
            }
        };

        this.model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE && !FactureUI.this.miseAJourEnCours) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    if (column == 2) {
                        FactureUI.this.mettreAJourQuantite(row);
                    } else if (column == 6) {
                        FactureUI.this.mettreAJourDepuisPUTTC(row);
                    }
                }
            }
        });

        this.factureManager = new FactureManager(this.model);
        this.factureManager.setModePrixGros(true);
        this.factureManager.setTimbre(1.0D);
        this.scanService = new ScanService();
    }

    private void loadData() {
        this.chargerClients();
        this.chargerArticles();
        this.chargerVoitures();
        this.chargerModesPaiement();
        this.genererNumeroFacture();

        this.comboClients.setSelectedIndex(-1);
        this.comboArticles.setSelectedIndex(-1);
        this.comboVoitures.setSelectedItem("");
        this.setupVoitureSearch();
    }

    private void initializeUI() {
        this.setTitle("Système de Facture - CHAA_ELECT");
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
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.4D; gbc.weighty = 1.0D;
        mainPanel.add(this.createClientPanel(), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6D;
        mainPanel.add(this.createArticlesPanel(), gbc);
        return mainPanel;
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

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 5;
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
            factureManager.setModePrixGros(true);
            afficherPrixArticle();
        });
        this.radioPrixDetail.addActionListener((e) -> {
            modePrixGros = false;
            factureManager.setModePrixGros(false);
            afficherPrixArticle();
        });

        panelModePrix.add(radioPrixGros);
        panelModePrix.add(radioPrixDetail);
        formPanel.add(panelModePrix, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("SCAN (F1) :", Font.BOLD, 12, new Color(231, 76, 60)), gbc);

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
            boolean success = scanService.traiterScan(code, model, factureManager, this);
            if (success) {
                calculerTotaux();
                txtScanCode.setText("");
                txtScanCode.requestFocusInWindow();
            } else {
                txtScanCode.selectAll();
            }
        });
        formPanel.add(this.txtScanCode, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("Recherche Article :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);

        gbc.gridx = 1; gbc.gridwidth = 4;
        this.comboArticles = new JComboBox<>();
        this.comboArticles.setEditable(true);
        this.comboArticles.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.comboArticles.addActionListener((e) -> { if (!isAdjusting) this.afficherPrixArticle(); });
        formPanel.add(this.comboArticles, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("Quantité :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        this.txtQuantite = this.createStyledTextField();
        this.txtQuantite.setPreferredSize(new Dimension(120, 30));
        this.txtQuantite.setText("1");
        this.txtQuantite.addActionListener((e) -> this.ajouterArticle());
        formPanel.add(this.txtQuantite, gbc);

        gbc.gridx = 3; gbc.gridwidth = 1;
        formPanel.add(this.createLabel("PU TTC :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);

        gbc.gridx = 4;
        this.txtPrix = this.createStyledTextField();
        this.txtPrix.setEditable(false);
        this.txtPrix.setBackground(new Color(245, 245, 245));
        formPanel.add(this.txtPrix, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 5;
        gbc.weightx = 1.0D; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAjouter = this.createStyledButton("AJOUTER À LA FACTURE", new Color(46, 204, 113));
        btnAjouter.setPreferredSize(new Dimension(250, 40));
        btnAjouter.addActionListener((e) -> this.ajouterArticle());
        formPanel.add(btnAjouter, gbc);

        panel.add(Box.createVerticalStrut(10));
        panel.add(formPanel);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void ajouterArticle() {
        try {
            String article = (String) this.comboArticles.getSelectedItem();
            if (article == null || article.isEmpty()) return;
            int qte = Integer.parseInt(this.txtQuantite.getText().trim());

            double prixTTC = modePrixGros
                ? DatabaseManager.getPrixGrosArticle(article)
                : DatabaseManager.getPrixDetailArticle(article);

            this.factureManager.ajouterArticle(article, qte, prixTTC, 0.0);
            this.calculerTotaux();
            this.txtQuantite.setText("1");
            this.txtQuantite.requestFocus();
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout d'un article", e);
        }
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

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0.3D;
        formPanel.add(this.createLabel("Client :", Font.BOLD, 12, new Color(60, 60, 60)), gbc);

        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.7D;
        this.comboClients = new JComboBox<>();
        this.comboClients.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
        this.panelClientInfosDB.add(this.createLabel("Matricule Fiscale :", Font.BOLD, 12, new Color(60, 60, 60)), gbcDB);
        gbcDB.gridx = 1; gbcDB.weightx = 0.7;
        this.txtMatricule = this.createStyledTextField();
        this.txtMatricule.setEditable(false);
        this.txtMatricule.setBackground(new Color(245, 245, 245));
        this.panelClientInfosDB.add(this.txtMatricule, gbcDB);

        this.panelPassagerInfos = new JPanel(new GridBagLayout());
        this.panelPassagerInfos.setBackground(new Color(240, 248, 255));
        this.panelPassagerInfos.setBorder(BorderFactory.createTitledBorder("Détails Passager (optionnel)"));
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
            "VÉHICULE (optionnel)",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(230, 126, 34)
        ));
        GridBagConstraints gbcVoit = new GridBagConstraints();
        gbcVoit.insets = new Insets(5, 5, 5, 5);
        gbcVoit.fill = GridBagConstraints.HORIZONTAL;

        gbcVoit.gridx = 0; gbcVoit.gridy = 0; gbcVoit.weightx = 0.3;
        this.panelVoitureInfos.add(this.createLabel("Matricule :", Font.BOLD, 12, new Color(60, 60, 60)), gbcVoit);
        gbcVoit.gridx = 1; gbcVoit.weightx = 0.7;
        this.comboVoitures = new JComboBox<>();
        this.comboVoitures.setEditable(true);
        this.comboVoitures.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.comboVoitures.setPreferredSize(new Dimension(200, 35));
        this.panelVoitureInfos.add(this.comboVoitures, gbcVoit);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4; gbc.weightx = 1.0;
        formPanel.add(this.panelClientInfosDB, gbc);
        gbc.gridy = 2;
        formPanel.add(this.panelPassagerInfos, gbc);
        gbc.gridy = 3;
        formPanel.add(this.panelVoitureInfos, gbc);

        gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAddClient = this.createStyledButton("Nouveau Client", new Color(46, 204, 113));
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

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(44, 62, 80));
        panel.setBorder(new CompoundBorder(
            new LineBorder(new Color(30, 40, 50), 1),
            new EmptyBorder(10, 15, 10, 15)
        ));
        panel.add(this.createBackButtonPanel(), BorderLayout.WEST);
        panel.add(this.createCompanyInfoPanel(), BorderLayout.CENTER);
        panel.add(this.createInvoiceInfoPanel(), BorderLayout.EAST);
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
        infoPanel.add(this.createLabel("Tél:94 226 752", Font.PLAIN, 11, new Color(200, 220, 255)));
        infoPanel.add(this.createLabel("MF: 000/M/A/1981916C", Font.PLAIN, 11, new Color(200, 220, 255)));

        leftPanel.add(infoPanel);
        return leftPanel;
    }

    private JPanel createInvoiceInfoPanel() {
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(44, 62, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        rightPanel.add(this.createLabel("FACTURE N°:", Font.BOLD, 11, Color.WHITE), gbc);
        gbc.gridx = 1;
        this.lblNumeroFacture = this.createLabel("", Font.BOLD, 13, new Color(255, 255, 150));
        rightPanel.add(this.lblNumeroFacture, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        rightPanel.add(this.createLabel("DATE:", Font.BOLD, 11, Color.WHITE), gbc);
        gbc.gridx = 1;
        this.txtDateFacture = new JTextField(LocalDate.now().toString());
        this.styleTextField(this.txtDateFacture);
        this.txtDateFacture.setPreferredSize(new Dimension(120, 25));
        rightPanel.add(this.txtDateFacture, gbc);

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
        this.tableDetails.setRowHeight(30);
        this.tableDetails.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        this.tableDetails.setGridColor(new Color(240, 240, 240));
        
        this.tableDetails.setSelectionBackground(new Color(51, 153, 255));
        this.tableDetails.setSelectionForeground(Color.BLACK);

        this.tableDetails.getColumnModel().getColumn(0).setPreferredWidth(80);
        this.tableDetails.getColumnModel().getColumn(1).setPreferredWidth(180);
        this.tableDetails.getColumnModel().getColumn(2).setPreferredWidth(50);
        this.tableDetails.getColumnModel().getColumn(3).setPreferredWidth(80);
        this.tableDetails.getColumnModel().getColumn(4).setPreferredWidth(100);
        this.tableDetails.getColumnModel().getColumn(5).setPreferredWidth(50);
        this.tableDetails.getColumnModel().getColumn(6).setPreferredWidth(90);
        this.tableDetails.getColumnModel().getColumn(7).setPreferredWidth(80);
        this.tableDetails.getColumnModel().getColumn(8).setPreferredWidth(100);

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
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(new Color(52, 73, 94));
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment(JLabel.CENTER);
                return this;
            }
        });
    }

    private void mettreAJourDepuisPUTTC(int row) {
        this.miseAJourEnCours = true;
        try {
            String designation = model.getValueAt(row, 1).toString();
            String qteStr = model.getValueAt(row, 2).toString().replaceAll("[^0-9]", "");
            int qte = 1;
            try { qte = Math.max(1, Integer.parseInt(qteStr)); } catch (NumberFormatException ignored) {}

            String puTTCStr = model.getValueAt(row, 6).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
            double nouveauPUTTC = Double.parseDouble(puTTCStr);

            double prixTTCBase = modePrixGros
                ? DatabaseManager.getPrixGrosArticle(designation)
                : DatabaseManager.getPrixDetailArticle(designation);

            String tvaStr = model.getValueAt(row, 5).toString().replaceAll("[^0-9]", "");
            int tva = Integer.parseInt(tvaStr);

            double puHT = nouveauPUTTC / (1.0 + tva / 100.0);
            
            double remisePct = 0.0;
            if (prixTTCBase > 0 && nouveauPUTTC < prixTTCBase) {
                remisePct = ((prixTTCBase - nouveauPUTTC) / prixTTCBase) * 100.0;
                remisePct = Math.round(remisePct * 100.0) / 100.0;
            }
            
            double totalTTCFinal = nouveauPUTTC * qte;
            double totalHTFinal = puHT * qte;

            model.setValueAt(formatMontantDinar(puHT), row, 3);
            model.setValueAt(formatMontantDinar(totalHTFinal), row, 4);
            model.setValueAt(formaterPourcentage(remisePct), row, 7);
            model.setValueAt(formatMontantDinar(totalTTCFinal), row, 8);

            this.calculerTotaux();

        } catch (Exception e) {
            log.error("Erreur lors du calcul du PU TTC", e);
            JOptionPane.showMessageDialog(this, "Erreur lors du calcul: " + e.getMessage());
        } finally {
            this.miseAJourEnCours = false;
        }
    }

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

            double prixTTCBase = modePrixGros
                ? DatabaseManager.getPrixGrosArticle(designation)
                : DatabaseManager.getPrixDetailArticle(designation);

            String remiseStr = model.getValueAt(row, 7).toString().replace("%", "").replace(",", ".").trim();
            double remiseExistante = 0.0;
            try { remiseExistante = Double.parseDouble(remiseStr); } catch (NumberFormatException ignored) {}

            String tvaStr = model.getValueAt(row, 5).toString().replaceAll("[^0-9]", "");
            int tva = Integer.parseInt(tvaStr);

            double puTTC = prixTTCBase * (1.0 - remiseExistante / 100.0);
            double puHT = puTTC / (1.0 + tva / 100.0);
            
            double totalTTC = puTTC * nouvelleQte;
            double totalHT = puHT * nouvelleQte;

            model.setValueAt(formatMontantDinar(puHT), row, 3);
            model.setValueAt(formatMontantDinar(totalHT), row, 4);
            model.setValueAt(formatMontantDinar(puTTC), row, 6);
            model.setValueAt(formatMontantDinar(totalTTC), row, 8);

            this.calculerTotaux();

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

        JButton btnViderFacture = this.createStyledButton("Nouvelle Facture", new Color(155, 89, 182));
        btnViderFacture.addActionListener((e) -> this.viderFacture());

        this.btnGenererFacture = this.createStyledButton("Valider et Imprimer", new Color(46, 204, 113));
        this.btnGenererFacture.addActionListener((e) -> this.genererFacture());

        panelButtons.add(btnSupprimer);
        panelButtons.add(btnViderFacture);
        panelButtons.add(this.btnGenererFacture);
        return panelButtons;
    }

    private JPanel createTotalPanel() {
        JPanel panelTotal = new JPanel(new BorderLayout(10, 5));
        panelTotal.setBackground(Color.WHITE);
        panelTotal.setBorder(new CompoundBorder(
            new MatteBorder(2, 0, 0, 0, new Color(52, 152, 219)),
            new EmptyBorder(10, 0, 5, 0)
        ));

        JPanel panelInfosFinancieres = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelInfosFinancieres.setBackground(Color.WHITE);

        panelInfosFinancieres.add(this.createLabel("REMISE ARTICLES:", Font.BOLD, 12, new Color(44, 62, 80)));
        this.lblMontantRemise = this.createLabel("0,000 DT", Font.BOLD, 12, new Color(192, 57, 43));
        panelInfosFinancieres.add(this.lblMontantRemise);

        panelInfosFinancieres.add(Box.createHorizontalStrut(20));
        panelInfosFinancieres.add(this.createLabel("TIMBRE:", Font.BOLD, 12, new Color(44, 62, 80)));
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1.0D, 0.0D, 100.0D, 0.5D);
        this.spinnerTimbre = new JSpinner(spinnerModel);
        this.spinnerTimbre.setPreferredSize(new Dimension(60, 25));
        this.spinnerTimbre.addChangeListener((e) -> {
            this.factureManager.setTimbre((Double) this.spinnerTimbre.getValue());
            this.calculerTotaux();
        });
        panelInfosFinancieres.add(this.spinnerTimbre);

        JPanel panelRetenue = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        panelRetenue.setBackground(new Color(255, 248, 220));
        panelRetenue.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(243, 156, 18), 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));

        this.chkRetenueSource = new JCheckBox("RETENUE À LA SOURCE");
        this.chkRetenueSource.setFont(new Font("Segoe UI", Font.BOLD, 12));
        this.chkRetenueSource.setForeground(new Color(150, 80, 0));
        this.chkRetenueSource.setBackground(new Color(255, 248, 220));

        this.comboTauxRetenue = new JComboBox<>(TAUX_RETENUE_LABELS);
        this.comboTauxRetenue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        this.comboTauxRetenue.setEnabled(false);
        this.comboTauxRetenue.setPreferredSize(new Dimension(230, 28));

        this.lblMontantRetenue = this.createLabel("0,000 DT", Font.BOLD, 13, new Color(150, 80, 0));
        this.lblMontantRetenue.setPreferredSize(new Dimension(120, 20));

        this.chkRetenueSource.addActionListener(e -> {
            comboTauxRetenue.setEnabled(chkRetenueSource.isSelected());
            calculerTotaux();
        });
        this.comboTauxRetenue.addActionListener(e -> calculerTotaux());

        panelRetenue.add(this.chkRetenueSource);
        panelRetenue.add(this.comboTauxRetenue);
        panelRetenue.add(this.createLabel("  Retenue :", Font.BOLD, 12, new Color(150, 80, 0)));
        panelRetenue.add(this.lblMontantRetenue);

        JPanel panelTotaux = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        panelTotaux.setBackground(Color.WHITE);

        panelTotaux.add(this.createLabel("HT:", Font.BOLD, 13, new Color(44, 62, 80)));
        this.lblTotalHT = this.createLabel("0,000", Font.BOLD, 14, new Color(52, 152, 219));
        panelTotaux.add(this.lblTotalHT);

        panelTotaux.add(Box.createHorizontalStrut(10));
        panelTotaux.add(this.createLabel("TVA:", Font.BOLD, 13, new Color(44, 62, 80)));
        this.lblTVA = this.createLabel("0,000", Font.BOLD, 14, new Color(230, 126, 34));
        panelTotaux.add(this.lblTVA);

        panelTotaux.add(Box.createHorizontalStrut(10));
        panelTotaux.add(this.createLabel("TOTAL TTC:", Font.BOLD, 14, new Color(44, 62, 80)));
        this.lblTotalTTC = this.createLabel("0,000", Font.BOLD, 16, new Color(46, 204, 113));
        panelTotaux.add(this.lblTotalTTC);

        panelTotaux.add(Box.createHorizontalStrut(20));
        panelTotaux.add(this.createLabel("NET À PAYER:", Font.BOLD, 14, new Color(44, 62, 80)));
        this.lblNetAPayer = this.createLabel("0,000", Font.BOLD, 17, new Color(22, 160, 133));
        this.lblNetAPayer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(22, 160, 133), 1),
            BorderFactory.createEmptyBorder(2, 10, 2, 10)
        ));
        panelTotaux.add(this.lblNetAPayer);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(Color.WHITE);
        wrapper.add(panelInfosFinancieres);
        wrapper.add(panelRetenue);
        wrapper.add(panelTotaux);

        panelTotal.add(wrapper, BorderLayout.CENTER);
        return panelTotal;
    }

    private void calculerTotaux() {
        double timbre = this.factureManager.getTimbre();
        CalculTotaux totaux = this.factureManager.calculerTotaux(timbre, 0.0);

        this.lblTotalHT.setText(formatMontantDinar(totaux.totalHT));
        this.lblTVA.setText(formatMontantDinar(totaux.totalTVA));
        this.lblTotalTTC.setText(formatMontantDinar(totaux.totalTTC));
        this.lblMontantRemise.setText(formatMontantDinar(totaux.totalRemise) + " DT");

        double montantRetenue = 0.0;
        double netAPayer = totaux.totalTTC;

        if (chkRetenueSource != null && chkRetenueSource.isSelected()) {
            if (totaux.totalHT >= SEUIL_RETENUE_HT) {
                int idx = comboTauxRetenue.getSelectedIndex();
                if (idx >= 0 && idx < TAUX_RETENUE.length) {
                    double taux = TAUX_RETENUE[idx];
                    montantRetenue = Math.round(totaux.totalHT * taux / 100.0 * 1000.0) / 1000.0;
                }
                netAPayer = totaux.totalTTC - montantRetenue;
            }
        }

        if (lblMontantRetenue != null)
            lblMontantRetenue.setText(formatMontantDinar(montantRetenue) + " DT");
        if (lblNetAPayer != null)
            lblNetAPayer.setText(formatMontantDinar(netAPayer));
    }

    private double getMontantRetenue() {
        if (chkRetenueSource == null || !chkRetenueSource.isSelected()) return 0.0;
        CalculTotaux totaux = this.factureManager.calculerTotaux(this.factureManager.getTimbre(), 0.0);
        if (totaux.totalHT < SEUIL_RETENUE_HT) return 0.0;
        int idx = comboTauxRetenue.getSelectedIndex();
        if (idx < 0 || idx >= TAUX_RETENUE.length) return 0.0;
        double taux = TAUX_RETENUE[idx];
        return Math.round(totaux.totalHT * taux / 100.0 * 1000.0) / 1000.0;
    }

    private void genererFacture() {
        if (factureEnCours) return;
        factureEnCours = true;
        btnGenererFacture.setEnabled(false);

        try {
            if (this.model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Ajoutez au moins un article");
                return;
            }

            String clientNomSelectionne = (String) this.comboClients.getSelectedItem();
            String nomClientFinal = "";
            int clientIdFinal = 0;
            String printNom = "", printPrenom = "", printAdresse = "", printTel = "", printMatricule = "";

            if ("Passager".equals(clientNomSelectionne)) {
                String nomSaisi = txtPassagerNom.getText().trim();
                String prenomSaisi = txtPassagerPrenom.getText().trim();
                if (nomSaisi.isEmpty() && prenomSaisi.isEmpty()) {
                    nomClientFinal = "Passager";
                    printNom = "Passager";
                } else {
                    nomClientFinal = nomSaisi + " " + prenomSaisi;
                    printNom = nomSaisi;
                    printPrenom = prenomSaisi;
                }
                printTel = txtPassagerTel.getText().trim();
                clientIdFinal = 0;
            } else {
                if (clientNomSelectionne == null) {
                    JOptionPane.showMessageDialog(this, "Veuillez sélectionner un client");
                    return;
                }
                nomClientFinal = clientNomSelectionne;
                clientIdFinal = DatabaseManager.getClientId(clientNomSelectionne);
                printNom = clientNomSelectionne;
                printAdresse = txtAdresse.getText();
                printMatricule = txtMatricule.getText();
            }

            String voitureSelectionnee = (String) this.comboVoitures.getSelectedItem();
            String modePaiement = this.comboModePaiement.getSelectedItem().toString();
            double timbre = this.factureManager.getTimbre();
            double remiseGlobale = 0.0;
            double retenueSource = getMontantRetenue();
            CalculTotaux totaux = this.factureManager.calculerTotaux(timbre, remiseGlobale);
            double netAPayer = totaux.totalTTC - retenueSource;

            if (chkRetenueSource.isSelected() && totaux.totalHT < SEUIL_RETENUE_HT) {
                Object[] optionsRetenue = {"Oui", "Non"};
                int rep = JOptionPane.showOptionDialog(this,
                    "La retenue est cochée mais le HT (" + formatMontantDinar(totaux.totalHT) +
                    " DT) est inferieur au seuil de " + (int) SEUIL_RETENUE_HT + " DT.\n" +
                    "Aucune retenue ne sera appliquée. Continuer ?",
                    "Retenue à la source",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null, optionsRetenue, optionsRetenue[1]);
                if (rep != 0) return;
            }

            // DEMANDE DE CONFIRMATION AVANT IMPRESSION
            Object[] optionsImpression = {"Oui", "Non"};
            int confirmation = JOptionPane.showOptionDialog(this,
                "Voulez-vous imprimer la facture ?",
                "Confirmation d'impression",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, optionsImpression, optionsImpression[0]);

            if (confirmation != 0) {
                factureEnCours = false;
                btnGenererFacture.setEnabled(true);
                return;
            }

            // CHOIX DU FORMAT D'IMPRESSION
            Object[] formatOptions = {"Format A4 (Facture)", "Format Ticket"};
            int choixFormat = JOptionPane.showOptionDialog(this,
                "Veuillez choisir le format d'impression :",
                "Format d'impression",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, formatOptions, formatOptions[0]);

            if (choixFormat == JOptionPane.CLOSED_OPTION) {
                factureEnCours = false;
                btnGenererFacture.setEnabled(true);
                return;
            }

            String numeroDocument;
            String typeDocument;

            if (choixFormat == 0) {
                typeDocument = "FACTURE";
                numeroDocument = this.lblNumeroFacture.getText();
            } else {
                typeDocument = "TICKET";
                this.genererNumeroTicket();
                numeroDocument = this.lblNumeroFacture.getText();
            }

            if (!this.factureManager.validerFacture(numeroDocument, this.txtDateFacture.getText(), nomClientFinal))
                return;

            int factureId = -1;

            if (modeModification && factureIdEnCours != -1) {
                boolean success = updateFactureExistante(
                    factureIdEnCours, nomClientFinal, clientIdFinal,
                    totaux, modePaiement, timbre, remiseGlobale,
                    retenueSource, netAPayer
                );
                if (success) factureId = factureIdEnCours;
            } else {
                if (numeroExisteDejaEnBase(numeroDocument)) {
                    JOptionPane.showMessageDialog(this,
                        "Ce numero de facture existe deja.\nVeuillez reessayer.",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                factureId = DatabaseManager.insererFactureAvecRetourId(
                    numeroDocument, typeDocument, this.txtDateFacture.getText(),
                    clientIdFinal, nomClientFinal, totaux.totalHT, totaux.totalTVA,
                    totaux.totalTTC, remiseGlobale, totaux.totalRemise, modePaiement, timbre,
                    printTel, retenueSource, netAPayer
                );

                if (factureId != -1) {
                    insererDetailsEtMajStock(factureId);
                    // Enregistrement dans l'historique des sorties (pour FACTURE et TICKET)
                    enregistrerSortiesHistorique(numeroDocument);
                }
            }

            if (factureId != -1) {
                JOptionPane.showMessageDialog(this, "Facture enregistree avec succes !");
                
                // IMPRESSION DE LA FACTURE
                boolean impressionReussie = false;
                try {
                    if (choixFormat == 0) {
                        // FACTURE A4 : TTC AVEC TIMBRE
                        FactureImpression impression = new FactureImpression(
                            numeroDocument, this.txtDateFacture.getText(),
                            printNom, printPrenom, printAdresse, printTel, printMatricule,
                            voitureSelectionnee, this.model,
                            formatMontantDinar(totaux.totalRemise), this.logoIcon,
                            modePaiement, timbre, retenueSource, netAPayer
                        );
                        impression.imprimer();
                        impressionReussie = true;
                    } else {
                        // TICKET : TTC SANS TIMBRE
                        double ttcSansTimbre = totaux.totalTTC - timbre;
                        
                        FactureImpressionTicket impressionTicket = new FactureImpressionTicket(
                            numeroDocument, this.txtDateFacture.getText(),
                            nomClientFinal, this.model, this.logoIcon,
                            modePaiement, 
                            totaux.totalHT,
                            ttcSansTimbre,
                            totaux.totalRemise, 
                            timbre
                        );
                        impressionTicket.imprimer();
                        impressionReussie = true;
                    }
                } catch (PrinterException ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Erreur lors de l'impression de la facture: " + ex.getMessage(),
                        "Erreur d'impression", JOptionPane.ERROR_MESSAGE);
                }
                
                // DEMANDE D'IMPRESSION DU BL PRÉREMPLI
                if (impressionReussie) {
                    Object[] optionsBL = {"Oui", "Non"};
                    int reponseBL = JOptionPane.showOptionDialog(this,
                        "Voulez-vous imprimer le Bon de Livraison accompagnant cette facture ?",
                        "Impression du Bon de Livraison",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, optionsBL, optionsBL[0]);

                    if (reponseBL == 0) {
                        imprimerBLPrerempli(numeroDocument, this.txtDateFacture.getText(),
                            printNom, printPrenom, printAdresse, printTel, printMatricule,
                            voitureSelectionnee, modePaiement, totaux, timbre);
                    }
                }
                
                this.viderFacture();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de l'enregistrement de la facture");
            }

        } catch (Exception e) {
            log.error("Erreur lors de la generation de la facture", e);
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage());
        } finally {
            factureEnCours = false;
            btnGenererFacture.setEnabled(true);
        }
    }

    /**
     * Enregistre les sorties d'articles dans l'historique
     * @param numeroDocument Numéro du document (facture ou ticket)
     */
    private void enregistrerSortiesHistorique(String numeroDocument) {
        List<String[]> lignes = new ArrayList<>();
        for (int i = 0; i < this.model.getRowCount(); i++) {
            String designation = this.model.getValueAt(i, 1).toString();
            String qteStr      = this.model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "");
            lignes.add(new String[]{designation, qteStr.isEmpty() ? "0" : qteStr});
        }
        boolean ok = DatabaseManager.enregistrerSortiesStockBatch(lignes, numeroDocument);
        if (ok) {
            log.info("Historique sorties enregistre pour: {}", numeroDocument);
        } else {
            log.error("Echec enregistrement historique sorties pour: {}", numeroDocument);
        }
    }

    private void imprimerBLPrerempli(String numeroFacture, String dateFacture,
                                      String nomClient, String prenomClient,
                                      String adresseClient, String telClient,
                                      String matriculeFiscale, String voiture,
                                      String modePaiement, CalculTotaux totaux,
                                      double timbre) {
        try {
            String numeroBL = "BL-" + numeroFacture;
            
            String nomChauffeur = "À déterminer";
            String adresseChauffeur = "";
            String telChauffeur = "";
            String voitureChauffeur = voiture != null ? voiture : "";
            
            BLImpression blImpression = new BLImpression(
                numeroBL,
                dateFacture,
                nomClient,
                prenomClient,
                adresseClient,
                telClient,
                matriculeFiscale,
                nomChauffeur,
                adresseChauffeur,
                telChauffeur,
                voitureChauffeur,
                this.model,
                formatMontantDinar(totaux.totalRemise),
                this.logoIcon,
                modePaiement
            );
            
            blImpression.imprimer();
            
            JOptionPane.showMessageDialog(this, 
                "Bon de Livraison imprimé avec succès !\nNuméro: " + numeroBL,
                "Succès", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de l'impression du Bon de Livraison: " + ex.getMessage(),
                "Erreur d'impression", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'impression du BL", e);
            JOptionPane.showMessageDialog(this,
                "Erreur inattendue lors de l'impression du BL: " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viderFacture() {
        this.model.setRowCount(0);
        this.lblTotalHT.setText("0,000");
        this.lblTVA.setText("0,000");
        this.lblTotalTTC.setText("0,000");
        this.lblMontantRemise.setText("0,000 DT");

        if (this.chkRetenueSource != null) this.chkRetenueSource.setSelected(false);
        if (this.comboTauxRetenue != null) { this.comboTauxRetenue.setEnabled(false); this.comboTauxRetenue.setSelectedIndex(0); }
        if (this.lblMontantRetenue != null) this.lblMontantRetenue.setText("0,000 DT");
        if (this.lblNetAPayer != null) this.lblNetAPayer.setText("0,000");

        this.txtPassagerNom.setText("");
        this.txtPassagerPrenom.setText("");
        this.txtPassagerTel.setText("");
        this.comboVoitures.setSelectedItem("");

        this.spinnerTimbre.setValue(1.0D);
        this.factureManager.setTimbre(1.0D);
        this.modeModification = false;
        this.factureIdEnCours = -1;
        this.setTitle("Système de Facture - CHAA ELECT");
        this.lblNumeroFacture.setForeground(new Color(255, 255, 150));

        this.genererNumeroFacture();

        this.comboClients.setSelectedIndex(-1);
        this.comboArticles.setSelectedIndex(-1);
        this.comboModePaiement.setSelectedIndex(-1);
        this.comboModePaiement.setSelectedItem("Espèces");

        this.txtPrix.setText("");
        this.txtQuantite.setText("1");
        this.txtDateFacture.setText(LocalDate.now().toString());
        this.effacerInfosClient();

        this.txtScanCode.setText("");
        this.txtScanCode.requestFocusInWindow();
    }

    private void genererNumeroFacture() {
        int annee = LocalDate.now().getYear();
        int seq = 1;
        String prefix = "FACT-" + annee + "-";
        Connection conn = null;
        try {
            conn = ConnexionSQLite.getConnection();
            try (PreparedStatement pst = conn.prepareStatement(
                "SELECT numero FROM Factures WHERE numero LIKE ? AND type = 'FACTURE' ORDER BY id DESC LIMIT 1")) {
                pst.setString(1, prefix + "%");
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    String[] p = rs.getString("numero").split("-");
                    try { seq = Integer.parseInt(p[p.length - 1]) + 1; } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la generation du numero de facture", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { log.error("Erreur fermeture connexion genererNumeroFacture", e); }
        }
        this.lblNumeroFacture.setText(prefix + String.format("%04d", seq));
    }

    private void genererNumeroTicket() {
        int annee = LocalDate.now().getYear();
        int seq = 1;
        String prefix = "TICKET-" + annee + "-";
        Connection conn = null;
        try {
            conn = ConnexionSQLite.getConnection();
            try (PreparedStatement pst = conn.prepareStatement(
                "SELECT numero FROM Factures WHERE numero LIKE ? AND type = 'TICKET' ORDER BY id DESC LIMIT 1")) {
                pst.setString(1, prefix + "%");
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    String[] p = rs.getString("numero").split("-");
                    try { seq = Integer.parseInt(p[p.length - 1]) + 1; } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la generation du numero de ticket", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { log.error("Erreur fermeture connexion genererNumeroTicket", e); }
        }
        this.lblNumeroFacture.setText(prefix + String.format("%04d", seq));
    }

    private boolean numeroExisteDejaEnBase(String numeroDocument) {
        String sql = "SELECT COUNT(*) FROM Factures WHERE numero = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, numeroDocument);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("Erreur verification numero: {}", e.getMessage(), e);
        }
        return false;
    }

    private void insererDetailsEtMajStock(int factureId) {
        for (int i = 0; i < this.model.getRowCount(); ++i) {
            try {
                String designation = this.model.getValueAt(i, 1).toString();
                int quantite = Integer.parseInt(this.model.getValueAt(i, 2).toString().replaceAll("[^0-9]", ""));
                double prixUnitaire = Double.parseDouble(
                    this.model.getValueAt(i, 6).toString().replace(",", ".").replaceAll("[^0-9.-]", ""));
                int tva = Integer.parseInt(this.model.getValueAt(i, 5).toString().replaceAll("[^0-9]", ""));

                DatabaseManager.insererDetailFacture(factureId, designation, quantite, prixUnitaire, tva);
                DatabaseManager.mettreAJourStock(designation, quantite);

            } catch (Exception e) {
                log.error("Erreur lors de l'insertion des details ou mise a jour du stock", e);
            }
        }
    }

    private void chargerClients() {
        this.comboClients.removeAllItems();
        this.comboClients.addItem("Passager");
        for (String client : DatabaseManager.chargerClients()) this.comboClients.addItem(client);
        this.comboClients.setSelectedIndex(-1);
    }

    private void chargerArticles() {
        this.cacheArticlesComplet = DatabaseManager.chargerArticles();
        this.comboArticles.removeAllItems();
        for (String article : cacheArticlesComplet) this.comboArticles.addItem(article);
        this.comboArticles.setSelectedIndex(-1);
        this.setupArticleSearch();
    }

    private void chargerVoitures() {
        this.cacheVoituresComplet = DatabaseManager.chargerToutesVoitures();
        this.comboVoitures.removeAllItems();
        this.comboVoitures.addItem("");
        for (String v : cacheVoituresComplet) this.comboVoitures.addItem(v);
        this.comboVoitures.setSelectedItem("");
        this.setupVoitureSearch();
    }

    private void chargerModesPaiement() {
        this.comboModePaiement.removeAllItems();
        this.comboModePaiement.addItem("Espèces");
        this.comboModePaiement.addItem("Echeance");
        this.comboModePaiement.addItem("Chèque");
        this.comboModePaiement.addItem("Virement");
        this.comboModePaiement.setSelectedItem("Espèces");
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
                    if (newModel.getSize() > 0) comboArticles.showPopup();
                    else comboArticles.hidePopup();
                    isAdjusting = false;
                });
            }
        });
    }

    private void setupVoitureSearch() {
        final JTextComponent editor = (JTextComponent) comboVoitures.getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN ||
                    e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_LEFT ||
                    e.getKeyCode() == KeyEvent.VK_RIGHT) return;
                SwingUtilities.invokeLater(() -> {
                    isAdjusting = true;
                    String text = editor.getText();
                    List<String> filtered = cacheVoituresComplet.stream()
                        .filter(item -> item.toLowerCase().contains(text.toLowerCase()))
                        .collect(Collectors.toList());
                    DefaultComboBoxModel<String> newModel = new DefaultComboBoxModel<>();
                    newModel.addElement("");
                    for (String item : filtered) newModel.addElement(item);
                    comboVoitures.setModel(newModel);
                    editor.setText(text);
                    if (newModel.getSize() > 1) comboVoitures.showPopup();
                    else comboVoitures.hidePopup();
                    isAdjusting = false;
                });
            }
        });
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

    private void chargerInfosClient() {
        if (this.comboClients.getSelectedItem() != null) {
            String[] infos = DatabaseManager.getInfosClientComplet(this.comboClients.getSelectedItem().toString());
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
            String article = item.toString();
            double prixTTC = modePrixGros
                ? DatabaseManager.getPrixGrosArticle(article)
                : DatabaseManager.getPrixDetailArticle(article);
            this.txtPrix.setText(formatMontantDinar(prixTTC));
        }
    }

    private void supprimerLigne() {
        int selectedRow = this.tableDetails.getSelectedRow();
        if (selectedRow != -1) {
            this.model.removeRow(selectedRow);
            this.calculerTotaux();
        } else {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une ligne à supprimer");
        }
    }

    private JPanel createBackButtonPanel() {
        JButton btn = this.createStyledButton("←", new Color(52, 152, 219));
        btn.setPreferredSize(new Dimension(40, 30));
        btn.addActionListener(e -> {
            new AdminDashboard().setVisible(true);
            this.dispose();
        });
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBackground(new Color(44, 62, 80));
        p.add(btn);
        return p;
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
            public void mouseEntered(MouseEvent evt) { button.setBackground(FactureUI.this.darkenColor(backgroundColor, 0.9F)); }
            public void mouseExited(MouseEvent evt)  { button.setBackground(backgroundColor); }
        });
        return button;
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        tf.setBackground(Color.WHITE);
        return tf;
    }

    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> cb = new JComboBox<>();
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setBackground(Color.WHITE);
        return cb;
    }

    private JLabel createLabel(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setBackground(new Color(60, 80, 100));
        tf.setForeground(Color.WHITE);
        tf.setEditable(false);
    }

    private Color darkenColor(Color color, float factor) {
        return new Color(
            Math.max((int)(color.getRed()   * factor), 0),
            Math.max((int)(color.getGreen() * factor), 0),
            Math.max((int)(color.getBlue()  * factor), 0)
        );
    }

    private String formaterPourcentage(double valeur) {
        if (valeur == (int) valeur) return (int) valeur + " %";
        return df.format(valeur) + " %";
    }

    private String formatMontantDinar(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df3 = new DecimalFormat("#,##0.000", symbols);
        String formatted = df3.format(montant).replace(",", ".");
        if (formatted.endsWith(".000")) return formatted.replace(".", ",");
        return formatted;
    }

    private void adjustComponentsForScreen() {
        if (Toolkit.getDefaultToolkit().getScreenSize().getWidth() < 1366) {
            this.tableDetails.getColumnModel().getColumn(1).setPreferredWidth(150);
        }
    }

    private void chargerLogo() {
        try {
            File f = new File("images/logo.png");
            if (f.exists()) {
                this.logoIcon = new ImageIcon(
                    new ImageIcon(f.getAbsolutePath()).getImage()
                        .getScaledInstance(60, 60, Image.SCALE_SMOOTH));
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement du logo", e);
        }
    }

    public void chargerFacturePourModification(Integer factureId) {
        if (factureId == null) {
            JOptionPane.showMessageDialog(this, "ID de facture invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        this.factureIdEnCours = factureId;
        this.modeModification = true;
        this.setTitle("Modification Facture - CHAA ELECT");
        this.lblNumeroFacture.setForeground(Color.RED);
        
        try (Connection conn = ConnexionSQLite.getConnection()) {
            String sqlFacture = "SELECT f.id, f.numero, f.date, f.client_id, f.client_nom, " +
                                "f.montant_ht, f.tva, f.montant_ttc, f.remise_pourcentage, " +
                                "f.montant_remise, f.moyen_paiement, f.timbre, f.tel_client, " +
                                "f.retenue_source, f.net_a_payer, " +
                                "c.adresse, c.matricule_fiscale " +
                                "FROM Factures f " +
                                "LEFT JOIN Clients c ON f.client_id = c.id " +
                                "WHERE f.id = ? AND f.type = 'FACTURE'";
            
            try (PreparedStatement pst = conn.prepareStatement(sqlFacture)) {
                pst.setInt(1, factureId);
                ResultSet rs = pst.executeQuery();
                
                if (rs.next()) {
                    this.lblNumeroFacture.setText(rs.getString("numero"));
                    
                    String dateStr = rs.getString("date");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            LocalDate date = LocalDate.parse(dateStr);
                            this.txtDateFacture.setText(date.toString());
                        } catch (Exception e) {
                            this.txtDateFacture.setText(dateStr);
                        }
                    }
                    
                    String modePaiement = rs.getString("moyen_paiement");
                    if (modePaiement != null) {
                        this.comboModePaiement.setSelectedItem(modePaiement);
                    }
                    
                    String clientNom = rs.getString("client_nom");
                    if (clientNom != null && !clientNom.isEmpty()) {
                        boolean clientExists = false;
                        for (int i = 0; i < this.comboClients.getItemCount(); i++) {
                            if (clientNom.equals(this.comboClients.getItemAt(i))) {
                                clientExists = true;
                                break;
                            }
                        }
                        if (!clientExists) {
                            this.comboClients.addItem(clientNom);
                        }
                        this.comboClients.setSelectedItem(clientNom);
                        
                        String adresse = rs.getString("adresse");
                        String matricule = rs.getString("matricule_fiscale");
                        if (adresse != null) this.txtAdresse.setText(adresse);
                        if (matricule != null) this.txtMatricule.setText(matricule);
                    } else {
                        this.comboClients.setSelectedIndex(0);
                    }
                    
                    double timbre = rs.getDouble("timbre");
                    this.spinnerTimbre.setValue(timbre);
                    this.factureManager.setTimbre(timbre);
                    
                    double retenueSource = rs.getDouble("retenue_source");
                    double montantHT = rs.getDouble("montant_ht");
                    if (retenueSource > 0 && montantHT > 0) {
                        this.chkRetenueSource.setSelected(true);
                        this.comboTauxRetenue.setEnabled(true);
                        double tauxCalcule = (retenueSource / montantHT) * 100.0;
                        for (int i = 0; i < TAUX_RETENUE.length; i++) {
                            if (Math.abs(TAUX_RETENUE[i] - tauxCalcule) < 0.3) {
                                this.comboTauxRetenue.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                    
                    String telClient = rs.getString("tel_client");
                    if (telClient != null && !telClient.isEmpty()) {
                        this.txtPassagerTel.setText(telClient);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Facture non trouvée", "Erreur", JOptionPane.ERROR_MESSAGE);
                    this.dispose();
                    return;
                }
            }
            
            String sqlDetails = "SELECT df.article_designation, df.quantite, df.prix_unitaire, df.tva " +
                                "FROM DetailsFacture df WHERE df.facture_id = ? " +
                                "GROUP BY df.article_designation";
            
            this.model.setRowCount(0);
            
            try (PreparedStatement pst = conn.prepareStatement(sqlDetails)) {
                pst.setInt(1, factureId);
                ResultSet rs = pst.executeQuery();
                
                while (rs.next()) {
                    String designation = rs.getString("article_designation");
                    int quantite = rs.getInt("quantite");
                    double prixUnitaireHT = rs.getDouble("prix_unitaire");
                    int tva = rs.getInt("tva");
                    
                    String reference = DatabaseManager.getReferenceArticle(designation);
                    if (reference == null || reference.isEmpty()) {
                        reference = "REF" + String.format("%04d", Math.abs(designation.hashCode() % 10000));
                    }
                    
                    double puTTC = prixUnitaireHT * (1.0 + tva / 100.0);
                    double totalHT = prixUnitaireHT * quantite;
                    double totalTTC = puTTC * quantite;
                    
                    Object[] rowData = {
                        reference,
                        designation,
                        quantite,
                        formatMontantDinar(prixUnitaireHT),
                        formatMontantDinar(totalHT),
                        tva + " %",
                        formatMontantDinar(puTTC),
                        "0 %",
                        formatMontantDinar(totalTTC)
                    };
                    this.model.addRow(rowData);
                }
            }
            
            this.calculerTotaux();
            this.miseAJourEnCours = false;
            
            JOptionPane.showMessageDialog(this, 
                "Facture chargée avec succès. Vous pouvez maintenant la modifier.", 
                "Modification", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (SQLException e) {
            log.error("Erreur lors du chargement de la facture pour modification", e);
            JOptionPane.showMessageDialog(this,
                "Erreur lors du chargement de la facture: " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
            this.dispose();
        }
    }

    private boolean updateFactureExistante(int factureId, String nomClient, int clientId,
            CalculTotaux totaux, String modePaiement, double timbre, double remiseGlobale,
            double retenueSource, double netAPayer) {
        Connection conn = null;
        try {
            conn = ConnexionSQLite.getConnection();
            conn.setAutoCommit(false);
            
            restaurerStockFacture(conn, factureId);
            
            String sqlDel = "DELETE FROM DetailsFacture WHERE facture_id=?";
            try (PreparedStatement pst = conn.prepareStatement(sqlDel)) {
                pst.setInt(1, factureId);
                pst.executeUpdate();
            }
            
            String sqlUpdate = "UPDATE Factures SET client_nom=?, client_id=?, montant_ht=?, tva=?, " +
                               "montant_ttc=?, montant_remise=?, moyen_paiement=?, timbre=?, date=?, " +
                               "retenue_source=?, net_a_payer=?, type='FACTURE' WHERE id=?";
            try (PreparedStatement pst = conn.prepareStatement(sqlUpdate)) {
                pst.setString(1, nomClient);
                pst.setInt(2, clientId);
                pst.setDouble(3, totaux.totalHT);
                pst.setDouble(4, totaux.totalTVA);
                pst.setDouble(5, totaux.totalTTC);
                pst.setDouble(6, totaux.totalRemise);
                pst.setString(7, modePaiement);
                pst.setDouble(8, timbre);
                pst.setString(9, this.txtDateFacture.getText());
                pst.setDouble(10, retenueSource);
                pst.setDouble(11, netAPayer);
                pst.setInt(12, factureId);
                pst.executeUpdate();
            }
            
            insererDetailsAvecConnexion(conn, factureId);
            
            conn.commit();
            return true;
            
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { log.error("Erreur lors du rollback", ex); }
            }
            log.error("Erreur lors de la modification de la facture", e);
            JOptionPane.showMessageDialog(this, "Erreur lors de la modification: " + e.getMessage(),
                                          "Erreur", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { log.error("Erreur fermeture connexion updateFactureExistante", e); }
            }
        }
    }

    private void insererDetailsAvecConnexion(Connection conn, int factureId) throws SQLException {
        String sqlIns = "INSERT INTO DetailsFacture (facture_id, article_designation, quantite, prix_unitaire, tva) VALUES (?, ?, ?, ?, ?)";
        String sqlStock = "UPDATE Articles SET stock = stock - ? WHERE designation = ?";
        
        try (PreparedStatement pstIns = conn.prepareStatement(sqlIns);
             PreparedStatement pstStock = conn.prepareStatement(sqlStock)) {
            
            for (int i = 0; i < this.model.getRowCount(); i++) {
                String designation = this.model.getValueAt(i, 1).toString();
                int quantite = Integer.parseInt(this.model.getValueAt(i, 2).toString().replaceAll("[^0-9]", ""));
                double prixUnitaire = Double.parseDouble(
                    this.model.getValueAt(i, 6).toString().replace(",", ".").replaceAll("[^0-9.-]", ""));
                int tva = Integer.parseInt(this.model.getValueAt(i, 5).toString().replaceAll("[^0-9]", ""));
                
                pstIns.setInt(1, factureId);
                pstIns.setString(2, designation);
                pstIns.setInt(3, quantite);
                pstIns.setDouble(4, prixUnitaire);
                pstIns.setInt(5, tva);
                pstIns.executeUpdate();
                
                pstStock.setInt(1, quantite);
                pstStock.setString(2, designation);
                pstStock.executeUpdate();
            }
        }
    }

    private void restaurerStockFacture(Connection conn, int factureId) throws SQLException {
        String sqlSelect = "SELECT article_designation, quantite FROM DetailsFacture WHERE facture_id = ?";
        String sqlUpdate = "UPDATE Articles SET stock = stock + ? WHERE designation = ?";
        
        try (PreparedStatement pstSelect = conn.prepareStatement(sqlSelect);
             PreparedStatement pstUpdate = conn.prepareStatement(sqlUpdate)) {
            
            pstSelect.setInt(1, factureId);
            ResultSet rs = pstSelect.executeQuery();
            
            while (rs.next()) {
                String designation = rs.getString("article_designation");
                int quantite = rs.getInt("quantite");
                
                pstUpdate.setInt(1, quantite);
                pstUpdate.setString(2, designation);
                pstUpdate.executeUpdate();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FactureUI().setVisible(true));
    }
}