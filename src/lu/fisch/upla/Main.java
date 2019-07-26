package lu.fisch.upla;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JOptionPane;

public class Main {

    private static String name = "Unimozer";
    private static String program = "Unimozer.jar";
    private static String programUri = "https://unimozer.fisch.lu/webstart/"+program;
    private static String md5Uri = "https://unimozer.fisch.lu/webstart/md5.php";
    private static String iconName = "unimozer.png";
    
    public static void main(String[] args) throws IOException
    {
        //JOptionPane.showMessageDialog(null, "Starting ...", "Error", JOptionPane.ERROR_MESSAGE);

        String path = "";
        try {
            path = Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString().replace("Upla.jar", "").replace("upla.jar", "").replace("file:/", "");
            //JOptionPane.showMessageDialog(null, path, "path", JOptionPane.ERROR_MESSAGE);
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error #main 1", JOptionPane.ERROR_MESSAGE);
        }
        
        if(path.startsWith("/"))
        {
            path = "/"+path;
        }
        
        //JOptionPane.showMessageDialog(null, path, "PATH", JOptionPane.ERROR_MESSAGE);

        //Ini.getInstance().save(); 
        Ini.getInstance().load();
        name = Ini.getInstance().getProperty("name", "Unimozer");
        program = Ini.getInstance().getProperty("program", "Unimozer.jar");
        programUri = Ini.getInstance().getProperty("programUri", "https://unimozer.fisch.lu/webstart/"+program);
        program = path+program;
        md5Uri = Ini.getInstance().getProperty("md5Uri", "https://unimozer.fisch.lu/webstart/md5.php");
        iconName = Ini.getInstance().getProperty("iconName", "unimozer.png");
        //Ini.getInstance().save();

        Launcher launcher = new Launcher();
        launcher.setIcon(new javax.swing.ImageIcon(launcher.getClass().getResource("/lu/fisch/upla/icons/"+iconName)));
        launcher.setName(name);
        launcher.setVisible(true);
        launcher.setLocationRelativeTo(null);
        launcher.setStatus("Loading ...");

        try 
        {
            File jar = new File(program);
            launcher.setStatus("Testing local cache ...");
            if(!jar.exists())
            {
                launcher.setStatus("Testing network ...");
                if(isOnline())
                {
                    launcher.setStatus("Downloading ...");
                    //launcher.setStatus(program);
                    download();
                    launcher.setStatus("Starting application ...");
                    start();
                }
                else
                {
                    JOptionPane.showMessageDialog(null, "The server can't be reached.\nPlease make shure you have an active internet connection ...", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            else
            {
                if(isOnline())
                {
                    if(!getLocalMD5().equals(getRemoteMD5()))
                    {
                        //launcher.setStatus(getLocalMD5()+" - "+getRemoteMD5());
                        launcher.setStatus("Downloading ... ");
                        download();
                    }
                }
                launcher.setStatus("Starting application ...");
                start();
            }
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error #main 2", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static boolean isOnline()
    {
        try {
            final URL url = new URL(md5Uri);
            url.openStream();
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
    
    private static String getRemoteMD5() throws MalformedURLException, IOException
    {
        // create a new trust manager that trust all certificates
        TrustManager[] trustAllCerts;
        trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // activate the new trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } 
        catch (KeyManagementException | NoSuchAlgorithmException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #getRemoteMD5", JOptionPane.ERROR_MESSAGE);
        }

        // dwonload the program
        URL url = new URL(md5Uri);
        BufferedReader in;
        in = new BufferedReader(new InputStreamReader(url.openStream()));

        String inputLine;
        String md5 = "";
        while ((inputLine = in.readLine()) != null)
        {
            md5+=inputLine;
        }
        in.close();;
        return md5.trim();
    }
    
    private static String getLocalMD5() throws NoSuchAlgorithmException, IOException
    {
        // get md5 hash of local file
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(Paths.get(program))) {
            DigestInputStream dis = new DigestInputStream(is, md);
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        StringBuilder checksumSb = new StringBuilder();
        for (byte digestByte : md.digest()) {
          checksumSb.append(String.format("%02x", digestByte));
        }
        return checksumSb.toString();
    }

    private static void download() throws MalformedURLException, IOException
    {
        // create a new trust manager that trust all certificates
        TrustManager[] trustAllCerts;
        trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // activate the new trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } 
        catch (KeyManagementException | NoSuchAlgorithmException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #download", JOptionPane.ERROR_MESSAGE);
        }

        // dwonload the program
        URL url = new URL(programUri);
        URLConnection connection = url.openConnection();
        OutputStream out;
        try (InputStream in = connection.getInputStream()) {
            //System.out.println(program);
            //JOptionPane.showMessageDialog(null, program, "program", JOptionPane.ERROR_MESSAGE);
            out = new FileOutputStream(new File(program));
            byte[] buffer = new byte[2048];
            int length;
            int downloaded = 0;
            while ((length = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, length);
                downloaded+=length;
            }
        }
        out.close();
    }
    
    private static void start() throws IOException, InterruptedException
    {
        // find javaw
        String bin = System.getProperty("file.separator")+"bin";
        boolean found=false;

        // get boot folder
        String bootFolder = System.getProperty("sun.boot.library.path");
        // go back two directories
        bootFolder=bootFolder.substring(0,bootFolder.lastIndexOf(System.getProperty("file.separator")));
        bootFolder=bootFolder.substring(0,bootFolder.lastIndexOf(System.getProperty("file.separator")));

        // get all files from the boot folder
        File bootFolderfile = new File(bootFolder);
        File[] files = bootFolderfile.listFiles();
        TreeSet<String> directories = new TreeSet<String>();
        for(int i=0;i<files.length;i++)
        {
            if(files[i].isDirectory()) directories.add(files[i].getAbsolutePath());
        }

        File javaw = null;
        while(directories.size()>0 && found==false)
        {
            String JDK_directory = directories.last();
            directories.remove(JDK_directory);   

            javaw = new File(JDK_directory+bin+System.getProperty("file.separator")+"java");
            if(javaw.exists()) break;
            javaw = new File(JDK_directory+bin+System.getProperty("file.separator")+"javaw");
            if(javaw.exists()) break;
            javaw = new File(JDK_directory+bin+System.getProperty("file.separator")+"javaw.exe");
            if(javaw.exists()) break;
        }

        if(javaw!=null)
        {
            // start it
            //System.out.println("Starting: "+javaw.getAbsolutePath()+" -jar "+program);
            if(System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
            {
               Process process = new ProcessBuilder(javaw.getAbsolutePath(),
                       "-jar",
                       "-Dapple.laf.useScreenMenuBar=true",
                       "-Dcom.apple.macos.use-file-dialog-packages=true",
                       "-Dcom.apple.macos.useScreenMenuBar=true",
                       "-Dcom.apple.smallTabs=true-Xmx1024M",
                       "-Dcom.apple.mrj.application.apple.menu.about.name="+name+"",
                       "-Dapple.awt.application.name="+name+"",
                       "-Xdock:name="+name,
                       program).start();
               //Process process = new ProcessBuilder(javaw.getAbsolutePath(),"-jar","-Dapple.laf.useScreenMenuBar=true",program).start();
            }
            else
            {
                Process process = new ProcessBuilder(javaw.getAbsolutePath(),"-jar",program).start();
            }
            try {
                // terminated this process
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #start", JOptionPane.ERROR_MESSAGE);
            }
            // terminated this process but wait a bit
            //Thread.sleep(1*1000);
            System.exit(0);
        }
    }
}
