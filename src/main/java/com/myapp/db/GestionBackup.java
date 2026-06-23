package com.myapp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(GestionBackup.class);
    private static final String DB_NAME = "gestion.db";
    private static final String CLOUD_FOLDER = "Backup_Facturation_Cloud";
    private static final String LOCAL_FOLDER = "Backup_Facturation_LOCAL";
    private static final int MAX_BACKUPS = 10;

    public static boolean effectuerBackup() {
        log.info("Demarrage du backup (Cloud + Local)");

        File sourceFile = trouverFichierSource();
        if (sourceFile == null) {
            log.error("Base de donnees introuvable");
            return false;
        }

        List<File> destinations = detecterDestinations();
        if (destinations.isEmpty()) {
            log.error("Aucun dossier de destination disponible");
            return false;
        }

        String backupName = genererNomFichier();
        int succes = 0;

        for (File dossier : destinations) {
            if (!dossier.exists()) {
                dossier.mkdirs();
            }
            if (copierFichier(sourceFile, dossier, backupName)) {
                succes++;
                nettoyerVieuxBackups(dossier);
            }
        }

        log.info("Backup termine ({} copies)", succes);
        return succes > 0;
    }

    public static boolean effectuerBackupSilencieux() {
        return effectuerBackup();
    }

    private static List<File> detecterDestinations() {
        List<File> liste = new ArrayList<>();
        String userHome = System.getProperty("user.home");

        String envOneDrive = System.getenv("OneDrive");
        if (envOneDrive != null && new File(envOneDrive).exists()) {
            File dossierCloud = new File(envOneDrive, CLOUD_FOLDER);
            liste.add(dossierCloud);
            log.info("Cible Cloud : {}", dossierCloud.getAbsolutePath());
        }

        File dossierLocal = new File(userHome, LOCAL_FOLDER);
        liste.add(dossierLocal);
        log.info("Cible Locale : {}", dossierLocal.getAbsolutePath());

        return liste;
    }

    private static File trouverFichierSource() {
        String[] chemins = {DB_NAME, "src/" + DB_NAME, System.getProperty("user.dir") + File.separator + DB_NAME};
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
            log.info("Backup OK -> {}", destFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            log.error("Echec copie vers : {}", dossierDest.getAbsolutePath(), e);
            return false;
        }
    }

    private static String genererNomFichier() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return "gestion_" + sdf.format(new Date()) + ".db";
    }

    private static void nettoyerVieuxBackups(File dossier) {
        File[] fichiers = dossier.listFiles((dir, name) -> name.endsWith(".db") && name.startsWith("gestion_"));

        if (fichiers != null && fichiers.length > MAX_BACKUPS) {
            Arrays.sort(fichiers, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            int aSupprimer = fichiers.length - MAX_BACKUPS;
            for (int i = 0; i < aSupprimer; i++) {
                if (fichiers[i].delete()) {
                    log.debug("Ancien backup supprime : {}", fichiers[i].getName());
                }
            }
        }
    }

    public static void main(String[] args) {
        effectuerBackup();
    }
}
