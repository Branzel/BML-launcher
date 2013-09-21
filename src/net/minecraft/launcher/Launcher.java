/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.minecraft.launcher;

import java.io.File;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author Branzel
 */
public class Launcher {
    
    public Launcher(JFrame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, Integer bootstrapVersion) {
        JOptionPane.showMessageDialog(frame, "Please update your launcher.", "Inane error", JOptionPane.ERROR_MESSAGE);
        File versions = new File(workingDirectory, "versions.zip");
        if (versions.isFile())
            versions.delete();
        File versionsnew = new File(workingDirectory, "versions.zip.new");
        if (versionsnew.isFile())
            versionsnew.delete();
        
    }
}
