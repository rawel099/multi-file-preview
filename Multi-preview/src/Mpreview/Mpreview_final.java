package Mpreview;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Mpreview_final {

    private static File currentFile;

    public static void main(String[] args) {

        JFrame frame = new JFrame("マルチファイルプレビュー");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 400);
        frame.setLayout(new BorderLayout());

        // プレビュー領域
        JLabel imagePreview = new JLabel("", SwingConstants.CENTER);
        JTextArea textPreview = new JTextArea();
        textPreview.setEditable(false);

        JPanel previewPanel = new JPanel(new CardLayout());
        previewPanel.add(imagePreview, "IMAGE");
        previewPanel.add(new JScrollPane(textPreview), "TEXT");

        // ボタン領域
        JButton openBtn = new JButton("開く");
        JButton openOtherBtn = new JButton("他のアプリで開く");
        JButton resetBtn = new JButton("アプリ初期化");

        openBtn.setEnabled(false);
        openOtherBtn.setEnabled(false);
        resetBtn.setEnabled(false);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(openBtn);
        bottomPanel.add(openOtherBtn);
        bottomPanel.add(resetBtn);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        // ファイル選択（フィルター）
        JFileChooser chooser = new JFileChooser();
        chooser.setControlButtonsAreShown(false);

        FileNameExtensionFilter imageFilter =
            new FileNameExtensionFilter("画像ファイル (*.jpg, *.png, *.gif)", "jpg", "png", "gif");
        FileNameExtensionFilter textFilter =
            new FileNameExtensionFilter("文章ファイル (*.txt, *.md, *.log)", "txt", "md", "log");
//        FileNameExtensionFilter allFilter =
//            new FileNameExtensionFilter("すべてのファイル (*.*)", "*");

        chooser.addChoosableFileFilter(imageFilter);
        chooser.addChoosableFileFilter(textFilter);
//        chooser.addChoosableFileFilter(allFilter);
//        chooser.setFileFilter(allFilter);

        frame.add(chooser, BorderLayout.WEST);
        frame.add(previewPanel, BorderLayout.CENTER);

        // 種類判定（Preferencesキー）
        java.util.function.Function<File, String> getTypeKey = (file) -> {
            String name = file.getName().toLowerCase();

            if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif"))
                return "defaultApp_image";

            if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".log"))
                return "defaultApp_text";

            // PDF / Word / Excel / その他はまとめて「other」
            if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx")
                || name.endsWith(".xls") || name.endsWith(".xlsx"))
                return "defaultApp_other";

            return "defaultApp_other";
        };

        Preferences prefs = Preferences.userRoot().node("MpreviewApp");

        // 開く（Windows標準 or 種類ごとの規定アプリ）
        openBtn.addActionListener(e -> {
            if (currentFile == null) return;

            String key = getTypeKey.apply(currentFile);
            String savedApp = prefs.get(key, null);

            try {
                if (savedApp != null) {
                    Runtime.getRuntime().exec(
                        "\"" + savedApp + "\" \"" + currentFile.getAbsolutePath() + "\""
                    );
                } else {
                    Desktop.getDesktop().open(currentFile);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // それ以外で開く（種類ごとに規定アプリ保存）
        openOtherBtn.addActionListener(e -> {
            if (currentFile == null) return;

            String key = getTypeKey.apply(currentFile);

            String[] apps = {
                "メモ帳",
                "Chrome",
                "Edge",
                "自分で選択"
            };

            JComboBox<String> combo = new JComboBox<>(apps);
            JCheckBox rememberCheck = new JCheckBox("次回からこの種類はこのアプリで開く");

            Object[] params = {
                "どのアプリで開きますか？",
                combo,
                rememberCheck
            };

            int result = JOptionPane.showConfirmDialog(
                frame, params, "プログラムから開く",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) return;

            String choice = (String) combo.getSelectedItem();
            String appPath = null;

            switch (choice) {
                case "メモ帳":
                    appPath = "notepad.exe";
                    break;
                case "Chrome":
                    appPath = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
                    break;
                case "Edge":
                    appPath = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
                    break;
                case "自分で選択":
                    JFileChooser fc = new JFileChooser();
                    fc.setDialogTitle("使用するアプリを選択してください");
                    fc.setFileFilter(new FileNameExtensionFilter("実行ファイル (*.exe)", "exe"));
                    if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
                        appPath = fc.getSelectedFile().getAbsolutePath();
                    break;
            }
            //プログラムの起動フラグの追加
            boolean success = true; 

            if (appPath == null) return;

            try {
                Runtime.getRuntime().exec(
                    "\"" + appPath + "\" \"" + currentFile.getAbsolutePath() + "\""
                );
            } catch (Exception ex) {
            	success = false;
            	JOptionPane.showMessageDialog(
            	        frame,
            	        "外部アプリの起動に失敗しました。\nパスが正しいか確認してください。",
            	        "起動エラー",
            	        JOptionPane.ERROR_MESSAGE
            	    );
            }

            if (success && rememberCheck.isSelected()) {
                prefs.put(key, appPath);
            }
        });

        // 規定アプリ初期化（種類 or 全部）
        resetBtn.addActionListener(e -> {
            if (currentFile == null) return;

            String key = getTypeKey.apply(currentFile);

            String typeName = switch (key) {
                case "defaultApp_image" -> "画像ファイル";
                case "defaultApp_text" -> "テキストファイル";
                default -> "その他のファイル";
            };

            Object[] options = {
                typeName + " のみ初期化",
                "すべて初期化",
                "キャンセル"
            };

            int choice = JOptionPane.showOptionDialog(
                frame,
                "どの規定アプリを初期化しますか？",
                "規定アプリの初期化",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );

            if (choice == 0) {
                // この種類だけ初期化
                prefs.remove(key);
                JOptionPane.showMessageDialog(
                    frame,
                    typeName + " の規定アプリを初期化しました。\n次回から Windows 標準のアプリで開きます。",
                    "初期化完了！",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } else if (choice == 1) {
                // 全種類初期化
                prefs.remove("defaultApp_image");
                prefs.remove("defaultApp_text");
                prefs.remove("defaultApp_other");

                JOptionPane.showMessageDialog(
                    frame,
                    "すべての種類の規定アプリを初期化しました。\n次回から Windows 標準のアプリで開きます。",
                    "全初期化完了‼",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        // ファイル選択時のプレビュー表示
        chooser.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
                    File file = (File) evt.getNewValue();
                    if (file == null || !file.exists()) return;

                    currentFile = file;
                    openBtn.setEnabled(true);
                    openOtherBtn.setEnabled(true);
                    resetBtn.setEnabled(true);

                    String name = file.getName().toLowerCase();

                    try {
                        if (name.endsWith(".jpg") || name.endsWith(".png")) {
                            // JPG / PNG：比率維持して縮小
                            ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                            Image img = icon.getImage();

                            int origW = img.getWidth(null);
                            int origH = img.getHeight(null);

                            int maxW = 300;
                            int maxH = 300;

                            double scale = Math.min((double) maxW / origW, (double) maxH / origH);

                            int newW = (int) (origW * scale);
                            int newH = (int) (origH * scale);

                            Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);

                            imagePreview.setIcon(new ImageIcon(scaled));
                            ((CardLayout) previewPanel.getLayout()).show(previewPanel, "IMAGE");

                        } else if (name.endsWith(".gif")) {
                            // GIF：スケーリングせずそのまま（アニメーション保持）
                            ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                            imagePreview.setIcon(icon);
                            ((CardLayout) previewPanel.getLayout()).show(previewPanel, "IMAGE");

                        } else if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".log")) {
                            // テキストプレビュー
                            BufferedReader br = new BufferedReader(new FileReader(file));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            int count = 0;

                            while ((line = br.readLine()) != null && count < 200) {
                                sb.append(line).append("\n");
                                count++;
                            }
                            br.close();

                            textPreview.setText(sb.toString());
                            ((CardLayout) previewPanel.getLayout()).show(previewPanel, "TEXT");

                        } else {
                            // それ以外はプレビュー不可
                            textPreview.setText("このファイルはプレビューできません。");
                            ((CardLayout) previewPanel.getLayout()).show(previewPanel, "TEXT");
                        }

                    } catch (Exception ex) {
                        textPreview.setText("読み込み中にエラーが発生しました。\n" + ex.getMessage());
                        ((CardLayout) previewPanel.getLayout()).show(previewPanel, "TEXT");
                    }
                }
            }
        });

        frame.setVisible(true);
    }
}
