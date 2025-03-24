package ch.RP.LauncherLib.Gui.web;

import javax.swing.*;
import java.awt.BorderLayout;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.callback.CefJSDialogCallback;
import org.cef.misc.BoolRef;
import org.cef.handler.CefJSDialogHandler.JSDialogType;

public class JCEFTest {
    public static void main(String[] args) {
        // Off-Screen Rendering deaktivieren
        System.setProperty("jcef.disable_osr", "true");

        SwingUtilities.invokeLater(() -> {
            // CefSettings konfigurieren – fensterbasiertes Rendering verwenden
            CefSettings settings = new CefSettings();
            settings.windowless_rendering_enabled = false;

            // Zusätzliche Kommandozeilen-Schalter zum Deaktivieren von GPU-Funktionen
            CefApp.addAppHandler(new CefAppHandlerAdapter(args) {
                @Override
                public void onBeforeCommandLineProcessing(String processType, org.cef.callback.CefCommandLine commandLine) {
                    commandLine.appendSwitch("disable-gpu");
                    commandLine.appendSwitch("disable-gpu-compositing");
                    commandLine.appendSwitch("disable-webgl");
                }
            });

            // JCEF initialisieren
            CefApp cefApp = CefApp.getInstance(args, settings);
            CefClient client = cefApp.createClient();

            // JSDialogHandler überschreiben, um native alert()-Aufrufe zu ersetzen
            client.addJSDialogHandler(new CefJSDialogHandlerAdapter() {
                @Override
                public boolean onJSDialog(CefBrowser browser,
                                          String origin_url,
                                          JSDialogType dialogType,
                                          String message_text,
                                          String default_prompt_text,
                                          CefJSDialogCallback callback,
                                          BoolRef suppress_message) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, message_text, "Alert", JOptionPane.INFORMATION_MESSAGE);
                    });
                    callback.Continue(true, null);
                    return true;
                }
            });

            // Browser erstellen und die Seite von localhost:8080 laden
            String url = "http://localhost:8080/";
            CefBrowser browser = client.createBrowser(url, false, false);

            // JFrame erstellen und den Browser einbetten
            JFrame frame = new JFrame("JCEF Test");
            frame.getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
