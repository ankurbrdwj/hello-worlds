/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ankur.webcurve.ui;


import com.ankur.webcurve.exchange.Exchange;

import javax.swing.*;

/**
 *
 * @author dennis
 */
public class ExchangeJApplet extends JApplet {

    /**
     * Initialization method that will be called after the applet is loaded
     * into the browser.
     */
    public void init() {
        // TODO start asynchronous download of heavy resources
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            	ExchangeJFrame frame = new ExchangeJFrame(new Exchange());
                frame.setVisible(true);            
            }
        });
    }

    // TODO overwrite start(), stop() and destroy() methods

}
