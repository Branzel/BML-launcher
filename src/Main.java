
import java.io.File;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import javax.swing.JFrame;
import net.branzel.launcher.Launcher;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Branzel
 */
public class Main {
    
    public static void main(String[] args) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();
                new Launcher(frame, new File("C:\\Users\\Branzel\\AppData\\Roaming\\BML"), Proxy.NO_PROXY, null, Integer.valueOf(2));
            }
        });
    }
}
