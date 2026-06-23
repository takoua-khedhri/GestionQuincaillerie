package com.myapp.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class GestionBackup {

    // --- CONFIGURATION ---
    private static final String DB_NAME = "gestion.db";
    
    // 1. Nom du dossier dans le CLOUD (OneDrive)
    private static final String CLOUD_FOLDER = "Backup_Facturation_Cloud";
    
    // 2. Nom du dossier sur le PC (Disque Dur)
    private static final String LOCAL_FOLDER = "Backup_Facturation_LOCAL";
    
    // Garder seulement les 10 derniers
    private static final int MAX_BACKUPS = 10; 

    public static boolean effectuerBackup() {
        System.out.println("\n=== 🚀 BACKUP STRICT (Cloud + Local) ===");
        
        File sourceFile = trouverFichierSource();
        if (sourceFile == null) {
            System.err.println("❌ ERREUR : Base de données introuvable !");
            return false;
        }

        List<File> destinations = detecterDestinations();
        
        if (destinations.isEmpty()) {
            System.err.println("❌ ERREUR GRAVE : Aucun dossier de destination n'a pu être créé !");
            return false;
        }

        String backupName = genererNomFichier();
        int succes = 0;

        for (File dossier : destinations) {
            // Création du dossier si inexistant
            if (!dossier.exists()) {
                dossier.mkdirs();
            }
            
            if (copierFichier(sourceFile, dossier, backupName)) {
                succes++;
                nettoyerVieuxBackups(dossier);
            }
        }

        System.out.println("=== FIN BACKUP (" + succes + " copies) ===\n");
        return succes > 0;
    }
    
    public static boolean effectuerBackupSilencieux() {
        return effectuerBackup();
    }

    // ========================================================================
    // 📍 DÉTECTION DES EMPLACEMENTS
    // ========================================================================
    private static List<File> detecterDestinations() {
        List<File> liste = new ArrayList<>();
        String userHome = System.getProperty("user.home"); // Ex: C:\Users\dell

        // --- 1. CLOUD (ONEDRIVE) ---
        String envOneDrive = System.getenv("OneDrive");
        if (envOneDrive != null && new File(envOneDrive).exists()) {
            File dossierCloud = new File(envOneDrive, CLOUD_FOLDER);
            liste.add(dossierCloud);
            System.out.println("☁️  Cible Cloud : " + dossierCloud.getAbsolutePath());
        }

        // --- 2. LOCAL (RACINE UTILISATEUR) ---
        // On force le chemin : C:\Users\dell\Backup_Facturation_LOCAL
        // On ne passe pas par "Documents" pour éviter les conflits
        File dossierLocal = new File(userHome, LOCAL_FOLDER);
        liste.add(dossierLocal);
        System.out.println("🏠 Cible Locale : " + dossierLocal.getAbsolutePath());

        return liste;
    }

    // --- UTILITAIRES ---

    private static File trouverFichierSource() {
        // Cherche le fichier source gestion.db
        String[] chemins = { DB_NAME, "src/" + DB_NAME, System.getProperty("user.dir") + File.separator + DB_NAME };
        for (String chemin : chemins) {
            File f = new File(chemin);
            if (f.exists()) return f;
        }
        return null;
    }

    private static boolean copierFichier(File source, File dossierDest, String nomFichier) {
        try {
            File destFile = new File(dossierDest, nomFichier);
            Files.copy(source.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ OK -> " + destFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("❌ Échec copie vers : " + dossierDest.getAbsolutePath());
            e.printStackTrace();
            return false;
        }
    }

    private static String genererNomFichier() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return "gestion_" + sdf.format(new Date()) + ".db";
    }

    // Garde uniquement les 10 derniers fichiers
    private static void nettoyerVieuxBackups(File dossier) {
        File[] fichiers = dossier.listFiles((dir, name) -> name.endsWith(".db") && name.startsWith("gestion_"));
        
        if (fichiers != null && fichiers.length > MAX_BACKUPS) {
            // Tri par date (ancien -> récent)
            Arrays.sort(fichiers, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            
            // Suppression des plus vieux
            int aSupprimer = fichiers.length - MAX_BACKUPS;
            for (int i = 0; i < aSupprimer; i++) {
                if(fichiers[i].delete()) {
                    System.out.println("🧹 Nettoyage (Ancien supprimé) : " + fichiers[i].getName());
                }
            }
        }
    }
    
    public static void main(String[] args) {
        effectuerBackup();
    }
}