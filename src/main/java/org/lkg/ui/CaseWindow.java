package org.lkg.ui;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.lkg.constant.UseCaseConstants;
import org.lkg.pattern.UserCaseObserver;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @description: 自定义swing界面
 * @author: 李开广
 * @date: 2023/5/13 11:12 PM
 */
public class CaseWindow extends JFrame implements UserCaseObserver {
    private static final Logger log = Logger.getLogger(CaseWindow.class.getSimpleName());

    private JPanel globalPanel;

    private JPanel buttonPane;

    private JRadioButton threePointFive = new JRadioButton("GPT-3.5");
    private JRadioButton four = new JRadioButton("GPT-4");
    private JPanel modelRadioPane;


    private JScrollPane jScrollPane;

    private JTextPane jTextPane;

    private StyleContext styleContext = new StyleContext();
    private Style redStyle;
    private Style commonStyle;


    private JProgressBar jProgressBar;

    private volatile boolean flag = true;

    private static volatile boolean isFour = false;

    public CaseWindow() {
        initGlobalPanel();
        initEvent();
    }

    private void initEvent() {
        threePointFive.addActionListener(e -> {
            isFour = false;
        });
        four.addActionListener(e -> {
            isFour = true;
        });
    }

    public static boolean isFour() {
        return isFour;
    }

    private void initChildrenComponentV1() {

        redStyle = styleContext.addStyle("red", null);
        StyleConstants.setForeground(redStyle, JBColor.RED);

        commonStyle = styleContext.addStyle("blue", null);
        StyleConstants.setForeground(commonStyle, Gray._255);

        jTextPane = new JTextPane();
        jTextPane.setEditable(false);

        threePointFive.setSelected(true);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(threePointFive);
        buttonGroup.add(four);
        this.modelRadioPane.add(threePointFive);
        this.modelRadioPane.add(four);

    }

    private void initGlobalPanel() {

        this.globalPanel = new JPanel(new BorderLayout());
        this.buttonPane = new JPanel(new BorderLayout());
        this.modelRadioPane = new JPanel(new FlowLayout());
        initChildrenComponentV1();
        this.jScrollPane = new JBScrollPane(this.jTextPane);
        this.jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.jProgressBar = new JProgressBar(0, 100);
        this.jProgressBar.setStringPainted(true);
        this.jProgressBar.setBackground(JBColor.RED);
        this.buttonPane.add(this.jProgressBar, BorderLayout.CENTER);
        this.globalPanel.add(this.buttonPane, BorderLayout.SOUTH);
        this.globalPanel.add(this.jScrollPane, BorderLayout.CENTER);
        this.globalPanel.add(this.modelRadioPane, BorderLayout.NORTH);

    }

    @Override
    public void update(String text, Integer type) {
        if (UseCaseConstants.TYPE_START_PROGRESS.equals(type)) {
            flag = true;
            startProcess();
            return;
        }
        if (matchTerminalCondition(text)) {
            finishProcess();
            return;
        }
        text = text.replaceAll("<Err >", "<Err>")
                .replaceAll("< Err >", "<Err>")
                .replaceAll("< Err>", "<Err>")
                .replaceAll("</Err >", "</Err>")
                .replaceAll("</ Err >", "</Err>")
                .replaceAll("</ Err>", "</Err>");

        int start = text.indexOf("<Err>");
        int end = text.indexOf("</Err>");
        jTextPane.setText("");
        log.info("渲染开始----------》");

        String temp = text;
        if (start == -1 || end == -1) {
            jTextPane.setText(text);
        } else {
            Document doc = jTextPane.getDocument();
//            try {
//                for (String s : needRenderTxtList) {
//                    doc.insertString(doc.getLength(), temp.substring(0, temp.indexOf(s)), commonStyle);
//                    doc.insertString(doc.getLength(), s, redStyle);
//                    temp = temp.substring(temp.indexOf(s));
//                }
//
//            } catch (BadLocationException e) {
//                e.printStackTrace();
//            }
////
            int minEnd = 0;
            try {
                while (start != -1 && end != -1) {

                    minEnd = Math.min(end + 6, temp.length() - 1);
                    doc.insertString(doc.getLength(), temp.substring(0, start), commonStyle);
                    doc.insertString(doc.getLength(), temp.substring(start, minEnd), redStyle);
//                    doc.insertString(doc.getLength(), temp.substring(minEnd), commonStyle);
                    // "<a>3</a> 2 <a>3</a>"
                    temp = temp.substring(minEnd);
                    start = temp.indexOf("<Err>");
                    end = temp.indexOf("</Err>");

                }
                doc.insertString(doc.getLength(), temp, commonStyle);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        this.globalPanel.repaint();
        finishProcess();
        log.info("渲染结束----------》");
    }

    private void startProcess() {
        jProgressBar.setValue(0);
        int maximum = jProgressBar.getMaximum();
        jProgressBar.repaint();
        new Thread(() -> {
            for (int i = 1; i <= maximum; i++) {
                if (!flag) {
                    break;
                }
                long l = (long) (Math.random() * 51 + 300);
                int val = i;
                if (maximum - jProgressBar.getValue() < 5 && Math.random() < 0.9d) {
                    val -= 1;
                }
                try {
                    jProgressBar.setValue(val);
                    jProgressBar.repaint();
                    TimeUnit.MILLISECONDS.sleep(l);
                } catch (InterruptedException ignored) {
                }

            }
        }).start();
    }

    private void finishProcess() {
        jProgressBar.setValue(jProgressBar.getMaximum());
        jProgressBar.repaint();
        this.flag = false;
    }

    private boolean matchTerminalCondition(String text) {
        return (Objects.isNull(text) || text.length() == 0) &&
                (jProgressBar.getValue() != jProgressBar.getMaximum());
    }

    public JPanel getGlobalPanel() {
        return globalPanel;
    }
}
