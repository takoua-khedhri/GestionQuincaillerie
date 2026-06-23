package com.myapp.db;

import com.myapp.models.Article;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    // =========================================================================
    // 1. GESTION DES CLIENTS
    // =========================================================================
   
    public static List<String> chargerClients() {
        List<String> clients = new ArrayList<>();
        String sql = "SELECT Nom, Prenom FROM Clients ORDER BY Nom";

        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String nom = rs.getString("Nom");
                String prenom = rs.getString("Prenom");
                if (prenom == null) prenom = "";
                
                String affichage = nom + (prenom.isEmpty() ? "" : " " + prenom);
                clients.add(affichage);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clients;
    }

    public static int getClientId(String nomClient) {
        String sql = "SELECT id FROM Clients WHERE Nom LIKE ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            String nomRecherche = nomClient.split(" ")[0]; 
            pst.setString(1, nomRecherche + "%");

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur ID Client: " + e.getMessage());
        }
        return -1;
    }

    public static String[] getInfosClientComplet(String nomAffichage) {
        String[] infos = {"", "", "", "", ""}; 
        String sql = "SELECT Nom, Prenom, Numero, adresse, matricule_fiscale FROM Clients WHERE Nom LIKE ?";

        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            String nom = nomAffichage.split(" ")[0];
            pst.setString(1, nom + "%");

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    infos[0] = rs.getString("Nom");
                    infos[1] = rs.getString("Prenom");
                    infos[2] = rs.getString("adresse") != null ? rs.getString("adresse") : "";
                    int tel = rs.getInt("Numero");
                    infos[3] = (tel != 0) ? String.valueOf(tel) : "";
                    infos[4] = rs.getString("matricule_fiscale") != null ? rs.getString("matricule_fiscale") : "";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return infos;
    }

    public static boolean clientExiste(String nom, String prenom, int numero) {
        String sql = "SELECT COUNT(*) FROM Clients WHERE nom = ? AND prenom = ? AND numero = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nom);
            pst.setString(2, prenom);
            pst.setInt(3, numero);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =========================================================================
    // 2. GESTION DES ARTICLES & STOCKS
    // =========================================================================
 
    public static List<String> chargerArticles() {
        List<String> articles = new ArrayList<>();
        String sql = "SELECT designation FROM Articles ORDER BY designation";

        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String designation = rs.getString("designation");
                if (designation != null && !designation.trim().isEmpty()) {
                    articles.add(designation);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articles;
    }

    public static Article getArticleByCode(String codeBarre) {
        String sql = "SELECT * FROM Articles WHERE code = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, codeBarre);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Article(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("designation"),
                        rs.getDouble("prix_gros"),
                        rs.getInt("stock"),
                        rs.getInt("tva")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur recherche par code: " + e.getMessage());
        }
        return null;
    }
    
    public static double getPrixGrosArticle(String designation) {
        String sql = "SELECT prix_gros FROM Articles WHERE designation = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, designation);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getDouble("prix_gros");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public static double getPrixDetailArticle(String designation) {
        String sql = "SELECT prix_detail FROM Articles WHERE designation = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, designation);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getDouble("prix_detail");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public static double getPrixArticle(String designation) {
        return getPrixGrosArticle(designation);
    }

    public static int getTauxTVAArticle(String designation) {
        return getTvaArticle(designation);
    }

    public static String getReferenceArticle(String designation) {
        String sql = "SELECT code, id FROM Articles WHERE designation = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, designation);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String code = rs.getString("code");
                    if (code != null && !code.isEmpty()) return code;
                    return String.format("REF%04d", rs.getInt("id"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "REF0000";
    }

    /**
     * Récupère l'ID d'un article par sa désignation
     */
    public static int getArticleIdByDesignation(String designation) {
        String sql = "SELECT id FROM Articles WHERE designation = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, designation);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getArticleIdByDesignation: " + e.getMessage());
        }
        return -1;
    }

    public static int getStockActuel(String designation) {
        String sql = "SELECT stock FROM Articles WHERE designation = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, designation);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("stock");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static boolean verifierStock(String designation, int quantiteDemandee) {
        return getStockActuel(designation) >= quantiteDemandee;
    }

    public static boolean mettreAJourStock(String designation, int quantiteVendue) {
        String sql = "UPDATE Articles SET stock = stock - ? WHERE designation = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, quantiteVendue);
            pst.setString(2, designation);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur MAJ Stock: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // 3. GESTION DES FACTURES
    // =========================================================================

    public static boolean insererFacture(String numero, String type, String date, 
                                         int clientId, String clientNom, 
                                         double montantHT, double tva, double montantTTC, 
                                         double remisePourcentage, double montantRemise, 
                                         String moyenPaiement, double timbre) {
        
        return insererFacture(numero, type, date, clientId, clientNom, 
                             montantHT, tva, montantTTC, remisePourcentage, 
                             montantRemise, moyenPaiement, timbre, "");
    }

    public static boolean insererFacture(String numero, String type, String date, 
                                         int clientId, String clientNom, 
                                         double montantHT, double tva, double montantTTC, 
                                         double remisePourcentage, double montantRemise, 
                                         String moyenPaiement, double timbre, String telClient) {
        
        String sql = "INSERT INTO Factures (numero, type, date, client_id, client_nom, " +
                     "montant_ht, tva, montant_ttc, remise_pourcentage, montant_remise, " +
                     "moyen_paiement, timbre, tel_client) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, numero);
            pst.setString(2, type);
            pst.setString(3, date);
            
            if (clientId <= 0) pst.setNull(4, java.sql.Types.INTEGER);
            else pst.setInt(4, clientId);
            
            pst.setString(5, clientNom);
            pst.setDouble(6, montantHT);
            pst.setDouble(7, tva);
            pst.setDouble(8, montantTTC);
            pst.setDouble(9, remisePourcentage);
            pst.setDouble(10, montantRemise);
            pst.setString(11, moyenPaiement);
            pst.setDouble(12, timbre);
            pst.setString(13, telClient != null ? telClient : "");

            return pst.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur Insertion " + type + ": " + e.getMessage());
            return false;
        }
    }

  public static int insererFactureAvecRetourId(String numero, String type, String date, 
                                             int clientId, String clientNom, 
                                             double montantHT, double tva, double montantTTC, 
                                             double remisePourcentage, double montantRemise, 
                                             String moyenPaiement, double timbre, String telClient,
                                             double retenueSource,   // nouveau
                                             double netAPayer) {     // nouveau
    
    String sql = "INSERT INTO Factures (numero, type, date, client_id, client_nom, " +
                 "montant_ht, tva, montant_ttc, remise_pourcentage, montant_remise, " +
                 "moyen_paiement, timbre, tel_client, retenue_source, net_a_payer) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    try (Connection conn = ConnexionSQLite.getConnection();
         PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        pst.setString(1, numero);
        pst.setString(2, type);
        pst.setString(3, date);
        
        if (clientId <= 0) pst.setNull(4, java.sql.Types.INTEGER);
        else pst.setInt(4, clientId);
        
        pst.setString(5, clientNom);
        pst.setDouble(6, montantHT);
        pst.setDouble(7, tva);
        pst.setDouble(8, montantTTC);
        pst.setDouble(9, remisePourcentage);
        pst.setDouble(10, montantRemise);
        pst.setString(11, moyenPaiement);
        pst.setDouble(12, timbre);
        pst.setString(13, telClient != null ? telClient : "");
        pst.setDouble(14, retenueSource);   // nouveau
        pst.setDouble(15, netAPayer);       // nouveau

        int affectedRows = pst.executeUpdate();
        
        if (affectedRows > 0) {
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;

    } catch (SQLException e) {
        System.err.println("❌ Erreur Insertion " + type + ": " + e.getMessage());
        return -1;
    }
}

    public static int getFactureId(String numeroFacture) {
        String sql = "SELECT id FROM Factures WHERE numero = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, numeroFacture);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public static boolean insererDetailFacture(int factureId, String designation, int quantite, double prixUnitaire, int tva) {
        String sql = "INSERT INTO DetailsFacture (facture_id, article_designation, quantite, prix_unitaire, tva) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            pst.setString(2, designation);
            pst.setInt(3, quantite);
            pst.setDouble(4, prixUnitaire);
            pst.setInt(5, tva);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur Insertion Détail: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // 4. CRUD COMPLET POUR LES ARTICLES
    // =========================================================================
  
    public static boolean ajouterArticle(String code, String designation, double prixGros, double prixDetail, int stock, int tva) {
        String sql = "INSERT INTO Articles(code, designation, prix_gros, prix_detail, stock, tva) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            if (code == null || code.isEmpty()) pst.setNull(1, java.sql.Types.VARCHAR);
            else pst.setString(1, code);
            
            pst.setString(2, designation);
            pst.setDouble(3, prixGros);
            pst.setDouble(4, prixDetail);
            pst.setInt(5, stock);
            pst.setInt(6, tva);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur Ajout Article: " + e.getMessage());
            return false;
        }
    }

    public static boolean modifierArticle(int id, String code, String designation, double prixGros, double prixDetail, int tva) {
        String sql = "UPDATE Articles SET code = ?, designation = ?, prix_gros = ?, prix_detail = ?, tva = ? WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, code);
            pst.setString(2, designation);
            pst.setDouble(3, prixGros);
            pst.setDouble(4, prixDetail);
            pst.setInt(5, tva);
            pst.setInt(6, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur Modif Article: " + e.getMessage());
            return false;
        }
    }

    public static boolean supprimerArticle(int id) {
        String sql = "DELETE FROM Articles WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur Suppr Article: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // 5. MÉTHODES UTILITAIRES POUR LA TVA
    // =========================================================================

    public static int getTvaArticle(String article) {
        String sql = "SELECT tva FROM Articles WHERE designation = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, article);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("tva");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération TVA: " + e.getMessage());
        }
        return 0;
    }

    // =========================================================================
    // 6. GESTION DES FOURNISSEURS
    // =========================================================================
    
    public static List<String> chargerFournisseursAvecDetails() {
        List<String> fournisseurs = new ArrayList<>();
        String sql = "SELECT nom, prenom, telephone, adresse FROM Fournisseurs ORDER BY nom, prenom";

        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                if (prenom == null) prenom = "";
                String telephone = rs.getString("telephone");
                if (telephone == null) telephone = "";
                String adresse = rs.getString("adresse");
                if (adresse == null) adresse = "";
                
                String affichage = nom + " " + prenom + " - " + telephone + " (" + adresse + ")";
                fournisseurs.add(affichage);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fournisseurs;
    }

    public static List<String> chargerFournisseurs() {
        List<String> fournisseurs = new ArrayList<>();
        String sql = "SELECT nom, prenom FROM Fournisseurs ORDER BY nom";

        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                if (prenom == null) prenom = "";
                
                String affichage = nom + (prenom.isEmpty() ? "" : " " + prenom);
                fournisseurs.add(affichage);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fournisseurs;
    }

    public static boolean fournisseurExiste(String nom, String prenom, String telephone, String adresse) {
        String sql = "SELECT COUNT(*) FROM Fournisseurs WHERE nom = ? AND (prenom = ? OR (prenom IS NULL AND ? IS NULL)) AND telephone = ? AND adresse = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, nom);
            
            if (prenom == null || prenom.isEmpty()) {
                pst.setNull(2, java.sql.Types.VARCHAR);
                pst.setNull(3, java.sql.Types.VARCHAR);
            } else {
                pst.setString(2, prenom);
                pst.setString(3, prenom);
            }
            
            pst.setString(4, telephone);
            pst.setString(5, adresse);
            
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String[] getInfosFournisseurCompletFromDisplay(String affichageComplet) {
        String[] infos = new String[6];
        
        try {
            String[] parts = affichageComplet.split(" - ");
            if (parts.length >= 2) {
                String nomPrenom = parts[0];
                String[] nomPrenomParts = nomPrenom.split(" ");
                if (nomPrenomParts.length >= 1) {
                    infos[0] = nomPrenomParts[0];
                    if (nomPrenomParts.length >= 2) {
                        StringBuilder prenom = new StringBuilder();
                        for (int i = 1; i < nomPrenomParts.length; i++) {
                            if (i > 1) prenom.append(" ");
                            prenom.append(nomPrenomParts[i]);
                        }
                        infos[1] = prenom.toString();
                    } else {
                        infos[1] = "";
                    }
                }
                
                String telAdresse = parts[1];
                String[] telAdresseParts = telAdresse.split(" \\(");
                if (telAdresseParts.length >= 2) {
                    infos[3] = telAdresseParts[0];
                    infos[2] = telAdresseParts[1].replace(")", "");
                } else {
                    infos[3] = telAdresseParts[0];
                    infos[2] = "";
                }
            }
            
            String query = "SELECT * FROM Fournisseurs WHERE nom = ? AND telephone = ? AND adresse = ?";
            try (Connection conn = ConnexionSQLite.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                
                pstmt.setString(1, infos[0]);
                pstmt.setString(2, infos[3]);
                pstmt.setString(3, infos[2]);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    infos[4] = rs.getString("matricule_fiscale") != null ? rs.getString("matricule_fiscale") : "";
                    infos[5] = rs.getString("email") != null ? rs.getString("email") : "";
                    if (infos[1].isEmpty() && rs.getString("prenom") != null) {
                        infos[1] = rs.getString("prenom");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return infos;
    }

    public static String[] getInfosFournisseurComplet(String nomAffichage) {
        String[] infos = {"", "", "", "", "", ""};
        
        String sql = "SELECT nom, prenom, adresse, telephone, matricule_fiscale, email FROM Fournisseurs WHERE nom || ' ' || COALESCE(prenom, '') = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, nomAffichage);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    infos[0] = rs.getString("nom");
                    infos[1] = rs.getString("prenom") != null ? rs.getString("prenom") : "";
                    infos[2] = rs.getString("adresse") != null ? rs.getString("adresse") : "";
                    infos[3] = rs.getString("telephone") != null ? rs.getString("telephone") : "";
                    infos[4] = rs.getString("matricule_fiscale") != null ? rs.getString("matricule_fiscale") : "";
                    infos[5] = rs.getString("email") != null ? rs.getString("email") : "";
                    return infos;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        String sql2 = "SELECT nom, prenom, adresse, telephone, matricule_fiscale, email FROM Fournisseurs WHERE nom LIKE ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql2)) {

            String nom = nomAffichage.split(" ")[0];
            pst.setString(1, nom + "%");

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    infos[0] = rs.getString("nom");
                    infos[1] = rs.getString("prenom") != null ? rs.getString("prenom") : "";
                    infos[2] = rs.getString("adresse") != null ? rs.getString("adresse") : "";
                    infos[3] = rs.getString("telephone") != null ? rs.getString("telephone") : "";
                    infos[4] = rs.getString("matricule_fiscale") != null ? rs.getString("matricule_fiscale") : "";
                    infos[5] = rs.getString("email") != null ? rs.getString("email") : "";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return infos;
    }

    public static int getFournisseurId(String nomFournisseur) {
        String sql = "SELECT id FROM Fournisseurs WHERE nom LIKE ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            String nomRecherche = nomFournisseur.split(" ")[0];
            pst.setString(1, nomRecherche + "%");

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur ID Fournisseur: " + e.getMessage());
        }
        return -1;
    }

    public static boolean insererFournisseur(String nom, String prenom, String adresse, 
                                             String telephone, String matricule, String email) {
        String sql = "INSERT INTO Fournisseurs (nom, prenom, adresse, telephone, matricule_fiscale, email) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, nom);
            pst.setString(2, prenom.isEmpty() ? null : prenom);
            pst.setString(3, adresse.isEmpty() ? null : adresse);
            pst.setString(4, telephone.isEmpty() ? null : telephone);
            pst.setString(5, matricule.isEmpty() ? null : matricule);
            pst.setString(6, email.isEmpty() ? null : email);

            return pst.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur insertion fournisseur: " + e.getMessage());
            return false;
        }
    }

    public static boolean ajouterFournisseurAvecVerification(String nom, String prenom, String telephone, 
                                                              String email, String adresse, String matricule) {
        if (fournisseurExiste(nom, prenom, telephone, adresse)) {
            return false;
        }
        return insererFournisseur(nom, prenom, adresse, telephone, matricule, email);
    }

    // =========================================================================
    // 7. GESTION DES BONS DE COMMANDE
    // =========================================================================

    public static boolean insererBonCommande(String numero, String date, 
                                             int fournisseurId, String fournisseurNom,
                                             double totalHT, double totalTTC, 
                                             double remisePourcentage, double montantRemise,
                                             String observations) {
        
        String sql = "INSERT INTO BonsCommande (numero, date, fournisseur_id, fournisseur_nom, total_ht, total_ttc, remise_pourcentage, montant_remise, observations) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, numero);
            pst.setString(2, date);
            
            if (fournisseurId <= 0) pst.setNull(3, java.sql.Types.INTEGER);
            else pst.setInt(3, fournisseurId);
            
            pst.setString(4, fournisseurNom);
            pst.setDouble(5, totalHT);
            pst.setDouble(6, totalTTC);
            pst.setDouble(7, remisePourcentage);
            pst.setDouble(8, montantRemise);
            pst.setString(9, observations);

            return pst.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur Insertion Bon de Commande: " + e.getMessage());
            return false;
        }
    }

    public static int getBonCommandeId(String numeroBonCommande) {
        String sql = "SELECT id FROM BonsCommande WHERE numero = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, numeroBonCommande);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return -1;
    }

    public static boolean insererDetailBonCommande(int bonCommandeId, String designation, int quantite, 
                                                    double prixAchat, double remiseLigne, double totalLigne) {
        String sql = "INSERT INTO DetailsBonCommande (bon_commande_id, article_designation, quantite, prix_achat, remise_pourcentage, total_ligne) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, bonCommandeId);
            pst.setString(2, designation);
            pst.setInt(3, quantite);
            pst.setDouble(4, prixAchat);
            pst.setDouble(5, remiseLigne);
            pst.setDouble(6, totalLigne);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur Insertion Détail Bon Commande: " + e.getMessage());
            return false;
        }
    }

    public static List<String> chargerTousArticles() {
        List<String> articles = new ArrayList<>();
        String sql = "SELECT designation FROM Articles ORDER BY designation";

        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String designation = rs.getString("designation");
                if (designation != null && !designation.trim().isEmpty()) {
                    articles.add(designation);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articles;
    }

    public static double getPrixAchatArticle(String designation) {
        return getPrixGrosArticle(designation) * 0.7;
    }

    public static List<String> getArticlesEnRupture() {
        List<String> articlesManquants = new ArrayList<>();
        String sql = "SELECT designation FROM Articles WHERE stock = 0 OR stock IS NULL ORDER BY designation";

        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                articlesManquants.add(rs.getString("designation"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articlesManquants;
    }

    public static List<String> getArticlesStockFaible(int seuil) {
        List<String> articlesFaibles = new ArrayList<>();
        String sql = "SELECT designation, stock FROM Articles WHERE stock <= ? AND stock > 0 ORDER BY stock";

        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, seuil);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    articlesFaibles.add(rs.getString("designation") + " (Stock: " + rs.getInt("stock") + ")");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articlesFaibles;
    }

    // =========================================================================
    // 8. MISE À JOUR STOCK APRÈS RÉCEPTION
    // =========================================================================
   
    public static boolean augmenterStock(String designation, int quantiteRecue) {
        String sql = "UPDATE Articles SET stock = stock + ? WHERE designation = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, quantiteRecue);
            pst.setString(2, designation);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur augmentation stock: " + e.getMessage());
            return false;
        }
    }

    public static boolean mettreAJourStatutBonCommande(String numeroBonCommande, String nouveauStatut) {
        String sql = "UPDATE BonsCommande SET statut = ? WHERE numero = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nouveauStatut);
            pst.setString(2, numeroBonCommande);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur mise à jour statut: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // 9. GESTION DES VOITURES
    // =========================================================================
 
    public static boolean ajouterVoiture(String matricule) {
        String sql = "INSERT INTO voitures (matricule, date_creation) VALUES (?, CURRENT_TIMESTAMP)";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, matricule.toUpperCase());
            return pst.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout voiture: " + e.getMessage());
            return false;
        }
    }
    
    public static int ajouterVoitureRetourId(String matricule) {
        String sql = "INSERT INTO voitures (matricule, date_creation) VALUES (?, CURRENT_TIMESTAMP)";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setString(1, matricule.toUpperCase());
            int affectedRows = pst.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout voiture avec retour ID: " + e.getMessage());
        }
        return -1;
    }

    public static boolean matriculeExiste(String matricule) {
        String sql = "SELECT COUNT(*) FROM voitures WHERE matricule = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, matricule.toUpperCase());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification matricule: " + e.getMessage());
        }
        return false;
    }

    public static int getVoitureId(String matricule) {
        String sql = "SELECT id FROM voitures WHERE matricule = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, matricule.toUpperCase());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération ID voiture: " + e.getMessage());
        }
        return -1;
    }

    public static String getMatriculeVoiture(int id) {
        String sql = "SELECT matricule FROM voitures WHERE id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("matricule");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération matricule: " + e.getMessage());
        }
        return null;
    }

    public static String getDateCreationVoiture(int id) {
        String sql = "SELECT date_creation FROM voitures WHERE id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("date_creation");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération date création: " + e.getMessage());
        }
        return null;
    }

    public static List<String> chargerToutesVoitures() {
        List<String> voitures = new ArrayList<>();
        String sql = "SELECT matricule FROM voitures ORDER BY matricule";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                voitures.add(rs.getString("matricule"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement voitures: " + e.getMessage());
        }
        return voitures;
    }

    public static List<Object[]> getToutesVoitures() {
        List<Object[]> voitures = new ArrayList<>();
        String sql = "SELECT id, matricule, date_creation FROM voitures ORDER BY matricule";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Object[] voiture = new Object[3];
                voiture[0] = rs.getInt("id");
                voiture[1] = rs.getString("matricule");
                voiture[2] = rs.getString("date_creation");
                voitures.add(voiture);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération voitures: " + e.getMessage());
        }
        return voitures;
    }

    public static boolean supprimerVoiture(int id) {
        String checkSql = "SELECT COUNT(*) FROM client_voiture WHERE voiture_id = ?";
        String deleteSql = "DELETE FROM voitures WHERE id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection()) {
            try (PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
                checkPst.setInt(1, id);
                try (ResultSet rs = checkPst.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        System.err.println("❌ Impossible de supprimer: voiture associée à des clients");
                        return false;
                    }
                }
            }
            
            try (PreparedStatement deletePst = conn.prepareStatement(deleteSql)) {
                deletePst.setInt(1, id);
                return deletePst.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression voiture: " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    // 10. GESTION DES RELATIONS CLIENT-VOITURE
    // =========================================================================
 
    public static boolean associerVoitureAuClient(int clientId, int voitureId) {
        String sql = "INSERT INTO client_voiture (client_id, voiture_id, date_association, est_active) VALUES (?, ?, CURRENT_TIMESTAMP, 1)";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, clientId);
            pst.setInt(2, voitureId);
            return pst.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur association client-voiture: " + e.getMessage());
            return false;
        }
    }

    public static List<String> getVoituresDuClient(int clientId) {
        List<String> voitures = new ArrayList<>();
        String sql = "SELECT v.matricule FROM voitures v " +
                     "INNER JOIN client_voiture cv ON v.id = cv.voiture_id " +
                     "WHERE cv.client_id = ? AND cv.est_active = 1 " +
                     "ORDER BY cv.date_association DESC";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, clientId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    voitures.add(rs.getString("matricule"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération voitures du client: " + e.getMessage());
        }
        return voitures;
    }

    public static List<Integer> getVoituresIdDuClient(int clientId) {
        List<Integer> voituresIds = new ArrayList<>();
        String sql = "SELECT voiture_id FROM client_voiture WHERE client_id = ? AND est_active = 1";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, clientId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    voituresIds.add(rs.getInt("voiture_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération IDs voitures du client: " + e.getMessage());
        }
        return voituresIds;
    }

    public static String getVoitureActiveDuClient(int clientId) {
        String sql = "SELECT v.matricule FROM voitures v " +
                     "INNER JOIN client_voiture cv ON v.id = cv.voiture_id " +
                     "WHERE cv.client_id = ? AND cv.est_active = 1 LIMIT 1";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, clientId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("matricule");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération voiture active: " + e.getMessage());
        }
        return null;
    }

    public static boolean desactiverAssociationVoiture(int clientId, int voitureId) {
        String sql = "UPDATE client_voiture SET est_active = 0, date_fin = CURRENT_TIMESTAMP " +
                     "WHERE client_id = ? AND voiture_id = ? AND est_active = 1";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, clientId);
            pst.setInt(2, voitureId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur désactivation association: " + e.getMessage());
            return false;
        }
    }

    public static boolean desactiverToutesVoituresDuClient(int clientId) {
        String sql = "UPDATE client_voiture SET est_active = 0, date_fin = CURRENT_TIMESTAMP " +
                     "WHERE client_id = ? AND est_active = 1";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, clientId);
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur désactivation toutes voitures: " + e.getMessage());
            return false;
        }
    }
    
    // =========================================================================
    // 11. GESTION DE L'HISTORIQUE (SUPPRESSION DÉFINITIVE)
    // =========================================================================

    public static boolean viderHistoriqueEntrees() {
        String sql = "DELETE FROM Historique_Entrees";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement()) {
            
            int lignesSupprimees = stmt.executeUpdate(sql);
            System.out.println("✅ Historique des entrées vidé : " + lignesSupprimees + " lignes supprimées");
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du vidage: " + e.getMessage());
            return false;
        }
    }

    public static boolean viderHistoriqueSorties() {
        String sql = "DELETE FROM Historique_Sorties";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement()) {
            
            int lignesSupprimees = stmt.executeUpdate(sql);
            System.out.println("✅ Historique des sorties vidé : " + lignesSupprimees + " lignes supprimées");
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du vidage: " + e.getMessage());
            return false;
        }
    }

    public static boolean viderTousHistoriques() {
        boolean successEntrees = viderHistoriqueEntrees();
        boolean successSorties = viderHistoriqueSorties();
        return successEntrees && successSorties;
    }

    public static int compterHistoriqueEntrees() {
        String sql = "SELECT COUNT(*) FROM Historique_Entrees";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur comptage: " + e.getMessage());
        }
        return -1;
    }

    public static int compterHistoriqueSorties() {
        String sql = "SELECT COUNT(*) FROM Historique_Sorties";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur comptage: " + e.getMessage());
        }
        return -1;
    }
    
    // =========================================================================
    // 12. GESTION DES ACHATS (FACTURES FOURNISSEURS)
    // =========================================================================
  
    public static Object[] getFournisseurCompletById(int fournisseurId) {
        String sql = "SELECT id, nom, prenom, adresse, telephone, matricule_fiscale, email, taux_retenue_source FROM Fournisseurs WHERE id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, fournisseurId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("adresse"),
                        rs.getString("telephone"),
                        rs.getString("matricule_fiscale"),
                        rs.getString("email"),
                        rs.getDouble("taux_retenue_source")
                    };
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération fournisseur: " + e.getMessage());
        }
        return null;
    }

    public static Object[] getFournisseurComplet(String nomFournisseur) {
        String sql = "SELECT id, nom, prenom, adresse, telephone, matricule_fiscale, email, taux_retenue_source FROM Fournisseurs WHERE nom = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, nomFournisseur);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("adresse"),
                        rs.getString("telephone"),
                        rs.getString("matricule_fiscale"),
                        rs.getString("email"),
                        rs.getDouble("taux_retenue_source")
                    };
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération fournisseur: " + e.getMessage());
        }
        return null;
    }

    public static List<String[]> chargerFournisseursComplet() {
        List<String[]> fournisseurs = new ArrayList<>();
        String sql = "SELECT id, nom, prenom,adresse, telephone, matricule_fiscale FROM Fournisseurs ORDER BY nom";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                String[] fournisseur = new String[5];
                fournisseur[0] = rs.getString("nom") + (rs.getString("prenom") != null ? " " + rs.getString("prenom") : "");
                fournisseur[1] = rs.getString("adresse") != null ? rs.getString("adresse") : "";
                fournisseur[2] = rs.getString("telephone") != null ? rs.getString("telephone") : "";
                fournisseur[3] = rs.getString("matricule_fiscale") != null ? rs.getString("matricule_fiscale") : "";
                fournisseur[4] = String.valueOf(rs.getInt("id"));
                fournisseurs.add(fournisseur);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement fournisseurs: " + e.getMessage());
        }
        return fournisseurs;
    }

   public static int insererFactureAchat(String numero, String dateFacture, String numeroFournisseur,
                                      int fournisseurId, double totalHT, double tva, double totalTTC,
                                      double retenuePct, double retenueMontant, double netAPayer,
                                      String statut) {

    String sql = "INSERT INTO FacturesAchat (numero, date_facture, numero_fournisseur, fournisseur_id, " +
                 "total_ht, tva, total_ttc, retenue_source_pourcentage, retenue_source_montant, net_a_payer, statut) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = ConnexionSQLite.getConnection();
         PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        pst.setString(1, numero);
        pst.setString(2, dateFacture);
        pst.setString(3, numeroFournisseur);
        pst.setInt(4, fournisseurId);
        pst.setDouble(5, totalHT);
        pst.setDouble(6, tva);
        pst.setDouble(7, totalTTC);
        pst.setDouble(8, retenuePct);
        pst.setDouble(9, retenueMontant);
        pst.setDouble(10, netAPayer);
        pst.setString(11, statut);

        int affectedRows = pst.executeUpdate();

        if (affectedRows > 0) {
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;

    } catch (SQLException e) {
        System.err.println("❌ Erreur insertion facture achat: " + e.getMessage());
        return -1;
    }
}

    public static boolean insererDetailFactureAchat(int factureAchatId, String designation, int quantite,
                                                    double prixUnitaireHT, double totalHT, int tvaPct) {
        
        double tvaMontant = totalHT * (tvaPct / 100.0);
        double totalTTC = totalHT + tvaMontant;
        
        String sql = "INSERT INTO DetailsAchat (facture_achat_id, article_designation, quantite, " +
                     "prix_unitaire_ht, total_ht, tva_pourcentage, tva_montant, total_ttc) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, factureAchatId);
            pst.setString(2, designation);
            pst.setInt(3, quantite);
            pst.setDouble(4, prixUnitaireHT);
            pst.setDouble(5, totalHT);
            pst.setInt(6, tvaPct);
            pst.setDouble(7, tvaMontant);
            pst.setDouble(8, totalTTC);
            
            return pst.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur insertion détail facture achat: " + e.getMessage());
            return false;
        }
    }

    public static List<Object[]> getToutesFacturesAchat() {
        List<Object[]> factures = new ArrayList<>();
        String sql = "SELECT fa.id, fa.numero, fa.numero_fournisseur, fa.date_facture, " +
                     "f.nom, f.prenom, fa.total_ht, fa.tva, fa.total_ttc, " +
                     "fa.retenue_source_pourcentage, fa.net_a_payer, fa.statut " +
                     "FROM FacturesAchat fa " +
                     "JOIN Fournisseurs f ON fa.fournisseur_id = f.id " +
                     "ORDER BY fa.date_facture DESC";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                Object[] facture = new Object[12];
                facture[0] = rs.getInt("id");
                facture[1] = rs.getString("numero");
                facture[2] = rs.getString("numero_fournisseur");
                facture[3] = rs.getString("date_facture");
                facture[4] = rs.getString("nom") + (rs.getString("prenom") != null ? " " + rs.getString("prenom") : "");
                facture[5] = rs.getDouble("total_ht");
                facture[6] = rs.getDouble("tva");
                facture[7] = rs.getDouble("total_ttc");
                facture[8] = rs.getDouble("retenue_source_pourcentage");
                facture[9] = rs.getDouble("net_a_payer");
                facture[10] = rs.getString("statut");
                facture[11] = rs.getString("numero");
                factures.add(facture);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération factures achat: " + e.getMessage());
        }
        return factures;
    }

    public static List<Object[]> getDetailsFactureAchat(int factureAchatId) {
        List<Object[]> details = new ArrayList<>();
        String sql = "SELECT article_designation, quantite, prix_unitaire_ht, total_ht, tva_pourcentage, total_ttc " +
                     "FROM DetailsAchat WHERE facture_achat_id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, factureAchatId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Object[] detail = new Object[6];
                    detail[0] = rs.getString("article_designation");
                    detail[1] = rs.getInt("quantite");
                    detail[2] = rs.getDouble("prix_unitaire_ht");
                    detail[3] = rs.getDouble("total_ht");
                    detail[4] = rs.getInt("tva_pourcentage");
                    detail[5] = rs.getDouble("total_ttc");
                    details.add(detail);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération détails: " + e.getMessage());
        }
        return details;
    }

    public static boolean updateStatutFactureAchat(int factureAchatId, String nouveauStatut) {
        String sql = "UPDATE FacturesAchat SET statut = ? WHERE id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, nouveauStatut);
            pst.setInt(2, factureAchatId);
            return pst.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur mise à jour statut: " + e.getMessage());
            return false;
        }
    }

    public static boolean supprimerFactureAchat(int factureAchatId) {
        String checkSql = "SELECT statut FROM FacturesAchat WHERE id = ?";
        String deleteSql = "DELETE FROM FacturesAchat WHERE id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection()) {
            try (PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
                checkPst.setInt(1, factureAchatId);
                try (ResultSet rs = checkPst.executeQuery()) {
                    if (rs.next() && "PAYEE".equals(rs.getString("statut"))) {
                        System.err.println("❌ Impossible de supprimer une facture déjà payée");
                        return false;
                    }
                }
            }
            
            try (PreparedStatement deletePst = conn.prepareStatement(deleteSql)) {
                deletePst.setInt(1, factureAchatId);
                return deletePst.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression: " + e.getMessage());
        }
        return false;
    }

    public static double getTotalAchatsParFournisseur(int fournisseurId) {
        String sql = "SELECT SUM(net_a_payer) as total FROM FacturesAchat WHERE fournisseur_id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, fournisseurId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur calcul total achats: " + e.getMessage());
        }
        return 0.0;
    }

    public static double getTotalRetenuesSource(String dateDebut, String dateFin) {
        String sql = "SELECT SUM(retenue_source_montant) as total FROM FacturesAchat " +
                     "WHERE date_facture BETWEEN ? AND ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, dateDebut);
            pst.setString(2, dateFin);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur calcul total retenues: " + e.getMessage());
        }
        return 0.0;
    }

    public static boolean ajouterStock(String designation, int quantite) {
        String sql = "UPDATE Articles SET stock = stock + ? WHERE designation = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, quantite);
            pst.setString(2, designation);
            return pst.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout stock: " + e.getMessage());
            return false;
        }
    }

    public static boolean enregistrerMouvementStock(String articleDesignation, String type, int quantite,
                                                     String referenceDocument, String typeDocument, String utilisateur) {
        String sql = "INSERT INTO MouvementsStock (article_designation, type, quantite, reference_document, type_document, utilisateur) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, articleDesignation);
            pst.setString(2, type);
            pst.setInt(3, quantite);
            pst.setString(4, referenceDocument);
            pst.setString(5, typeDocument);
            pst.setString(6, utilisateur);
            
            return pst.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur enregistrement mouvement: " + e.getMessage());
            return false;
        }
    }

    public static List<Object[]> getMouvementsStockParArticle(String designation) {
        List<Object[]> mouvements = new ArrayList<>();
        String sql = "SELECT id, type, quantite, reference_document, type_document, date_mouvement, utilisateur " +
                     "FROM MouvementsStock WHERE article_designation = ? ORDER BY date_mouvement DESC";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, designation);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Object[] mouvement = new Object[7];
                    mouvement[0] = rs.getInt("id");
                    mouvement[1] = rs.getString("type");
                    mouvement[2] = rs.getInt("quantite");
                    mouvement[3] = rs.getString("reference_document");
                    mouvement[4] = rs.getString("type_document");
                    mouvement[5] = rs.getString("date_mouvement");
                    mouvement[6] = rs.getString("utilisateur");
                    mouvements.add(mouvement);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération mouvements: " + e.getMessage());
        }
        return mouvements;
    }

    public static boolean fournisseurExisteParMatricule(String nom, String matriculeFiscale) {
        if (matriculeFiscale == null || matriculeFiscale.isEmpty()) {
            return false;
        }
        
        String sql = "SELECT COUNT(*) FROM Fournisseurs WHERE nom = ? AND matricule_fiscale = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, nom);
            pst.setString(2, matriculeFiscale);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification fournisseur: " + e.getMessage());
        }
        return false;
    }

    public static boolean updateTauxRetenueFournisseur(int fournisseurId, double tauxReelNouveau) {
        String sql = "UPDATE Fournisseurs SET taux_retenue_source = ? WHERE id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setDouble(1, tauxReelNouveau);
            pst.setInt(2, fournisseurId);
            return pst.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur mise à jour taux retenue: " + e.getMessage());
        }
        return false;
    }
     public static boolean enregistrerSortieStock(String designation, int quantite, String numeroFact) {
        // 1. Récupérer l'ID de l'article
        int articleId = getArticleIdByDesignation(designation);
        if (articleId == -1) {
            System.err.println("❌ Article non trouvé pour la sortie: " + designation);
            return false;
        }
 
        String sql = "INSERT INTO Historique_Sorties (article_id, designation, quantite, numero_fact) " +
                     "VALUES (?, ?, ?, ?)";
 
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
 
            pst.setInt(1, articleId);
            pst.setString(2, designation);
            pst.setInt(3, quantite);
            pst.setString(4, numeroFact != null && !numeroFact.isEmpty() ? numeroFact : "Manuelle");
            return pst.executeUpdate() > 0;
 
        } catch (SQLException e) {
            System.err.println("❌ Erreur enregistrement sortie: " + e.getMessage());
            return false;
        }
    }
 
    /**
     * Enregistre plusieurs sorties en batch (pour Facture / BL / Ticket).
     * Utilise une seule connexion avec transaction pour la performance.
     * @param lignes      Liste de {designation, quantite}
     * @param numeroDoc   Numéro du document (FACT-xxx, BL-xxx, TICKET-xxx)
     * @return true si toutes les insertions ont réussi
     */
    public static boolean enregistrerSortiesStockBatch(List<String[]> lignes, String numeroDoc) {
        String sql = "INSERT INTO Historique_Sorties (article_id, designation, quantite, numero_fact) " +
                     "VALUES (?, ?, ?, ?)";
 
        try (Connection conn = ConnexionSQLite.getConnection()) {
            conn.setAutoCommit(false);
 
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                for (String[] ligne : lignes) {
                    String designation = ligne[0];
                    int quantite = Integer.parseInt(ligne[1]);
                    int articleId = getArticleIdByDesignation(designation);
 
                    if (articleId == -1) {
                        System.err.println("⚠️ Article ignoré (non trouvé en base): " + designation);
                        continue;
                    }
 
                    pst.setInt(1, articleId);
                    pst.setString(2, designation);
                    pst.setInt(3, quantite);
                    pst.setString(4, numeroDoc);
                    pst.addBatch();
                }
                pst.executeBatch();
                conn.commit();
                System.out.println("✅ Sorties historique enregistrées pour: " + numeroDoc);
                return true;
 
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("❌ Erreur batch sorties: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur connexion batch: " + e.getMessage());
            return false;
        }
    }
}