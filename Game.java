import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Zombileri Vur!");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(Color.GREEN);

        Menü menü = new Menü();
        GamePanel game = new GamePanel();
        game.setVisible(false);
        game.setPaused(true);
        Difficulty difficulty = new Difficulty(game);
        difficulty.setVisible(false);

        JButton playButton = new JButton("Oyna");
        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                menü.setVisible(false);
                difficulty.setVisible(true);
            }

        });
        menü.add(playButton);

        JButton controlButton = new JButton("Kontoller");
        controlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // controls
            }
        });

        JButton loadButton = new JButton("Load Game");
        loadButton.setBounds(300, 400, 200, 50);
        loadButton.addActionListener(e -> {
            String fileName = "save.txt";
            if (fileName != null && !fileName.isEmpty()) {
                game.loadGame(fileName);
            }
        });
        menü.add(loadButton);

        menü.add(controlButton);
        menü.setVisible(true);
        frame.add(menü);
        frame.add(difficulty);
        frame.add(game);

        frame.setVisible(true);

    }
}

class GamePanel extends JPanel implements ActionListener {

    private Oyuncu oyuncu;
    private List<Zombiler> zombiler;
    private int wave = 1;
    private boolean Paused = false;
    private Timer timer;

    private JLabel bildirimpaneli;
    private JLabel silahpaneli;
    private JLabel canpaneli;
    private JPanel pausepaneli;

    private List<Mermi> mermiler;
    private List<PickUpAble> loots;
    private int difficulty;

    private JLayeredPane pane;

    private int mouseX;
    private int mouseY;

    GamePanel() {
        setSize(800, 600);
        setBackground(Color.WHITE);

        // Bildirim paneli
        bildirimpaneli = new JLabel();
        bildirimpaneli.setFont(new Font("Arial", Font.BOLD, 16));
        // bildirimpaneli.setHorizontalAlignment(SwingConstants.CENTER);
        bildirimpaneli.setForeground(Color.GREEN);
        add(bildirimpaneli, BorderLayout.NORTH);

        // Silah ve mermi gösteren panel
        silahpaneli = new JLabel();
        silahpaneli.setFont(new Font("Arial", Font.BOLD, 16));
        silahpaneli.setForeground(Color.BLACK);
        add(silahpaneli, BorderLayout.NORTH);

        // Can paneli
        canpaneli = new JLabel();
        canpaneli.setFont(new Font("Arial", Font.BOLD, 16));
        canpaneli.setForeground(Color.RED);
        add(canpaneli, BorderLayout.SOUTH);

        // Pause paneli
        pane = new JLayeredPane();
        pane.setSize(800, 600);
        pane.add(this, JLayeredPane.DEFAULT_LAYER);

        pausepaneli = new JPanel();
        pausepaneli.setPreferredSize(new Dimension(600, 450));
        pausepaneli.setBackground(Color.GREEN);
        pausepaneli.setLayout(new GridLayout(3, 1));

        JLabel pauseL = new JLabel("Paused!", SwingConstants.CENTER);
        pausepaneli.add(pauseL);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveGame("save.txt");
                JOptionPane.showMessageDialog(null, "Oyun Kaydedeildi!");
            }

        });
        pausepaneli.add(saveButton);

        JButton exitButton = new JButton("Çıkış");
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int cikis = JOptionPane.showConfirmDialog(null, "Emin misin?", "Çıkış", JOptionPane.YES_NO_OPTION);
                if (cikis == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });
        pausepaneli.add(exitButton);

        pausepaneli.setVisible(false);
        add(pausepaneli);

        Tabanca tabanca1 = new Tabanca();
        oyuncu = new Oyuncu(400, 300, tabanca1);
        oyuncu.envanter.addItem(0, tabanca1);
        silahUpdate();
        zombiler = new ArrayList<>();
        mermiler = oyuncu.silah.mermiler;
        loots = new ArrayList<>();
        zombiSpawnla();

        timer = new Timer(1000 / 600, this);
        timer.start();

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent m) {
                if (SwingUtilities.isLeftMouseButton(m)) {
                    oyuncu.ates(zombiler, mouseX, mouseY);
                    silahUpdate();
                }
            }
        });

        addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent k) {

                if (k.getKeyCode() == KeyEvent.VK_W) {
                    oyuncu.move(0, -1);
                }
                if (k.getKeyCode() == KeyEvent.VK_A) {
                    oyuncu.move(-1, 0);
                }
                if (k.getKeyCode() == KeyEvent.VK_S) {
                    oyuncu.move(0, 1);
                }
                if (k.getKeyCode() == KeyEvent.VK_D) {
                    oyuncu.move(1, 0);
                }
                if (k.getKeyCode() == KeyEvent.VK_R) {
                    oyuncu.silahReload();
                    oyuncu.silah.mermiUpdate();
                    bildirimpaneli.setText("Şarjör Dolduruldu");
                    silahUpdate();
                }
                if (k.getKeyCode() == KeyEvent.VK_1) {
                    oyuncu.silahDeğiş(new Tabanca());
                    oyuncu.silah.mermiUpdate();
                    bildirimEkle("Silah Değiştirildi");
                    silahUpdate();
                }
                if (k.getKeyCode() == KeyEvent.VK_2) {
                    if (wave > 1) {
                        PiyadeTüfeği silah = (PiyadeTüfeği) oyuncu.envanter.get(1);
                        oyuncu.silahDeğiş(silah);
                        oyuncu.silah.mermiUpdate();
                        bildirimEkle("Silah Değiştirildi!");
                        silahUpdate();
                    }
                }
                if (k.getKeyCode() == KeyEvent.VK_3) {
                    if (wave > 3) {
                        oyuncu.silahDeğiş(new PompaliTüfek());
                        oyuncu.silah.mermiUpdate();
                        bildirimEkle("Silah Değiştirildi");
                        silahUpdate();
                    }
                }
                if (k.getKeyCode() == KeyEvent.VK_4) {
                    if (wave > 5) {
                        oyuncu.silahDeğiş(new KeskinNişanciTüfeği());
                        oyuncu.silah.mermiUpdate();
                        bildirimEkle("Silah Değiştirildi");
                        silahUpdate();
                    }
                }
                if (k.getKeyCode() == KeyEvent.VK_5) {
                    if (wave > 10) {
                        oyuncu.silahDeğiş(new RoketAtar());
                        oyuncu.silah.mermiUpdate();
                        bildirimEkle("Silah Değiştirildi");
                        silahUpdate();
                    }
                }
                if (k.getKeyCode() == KeyEvent.VK_F) {
                    for (PickUpAble loot : loots) {
                        loot.pickUp(oyuncu);
                    }
                }
                if (k.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    Paused = !Paused;
                    if (Paused) {
                        pausepaneli.setVisible(true);
                    } else {
                        pausepaneli.setVisible(false);
                        requestFocusInWindow();
                    }

                }
                pane.repaint();
            }

        });
        setFocusable(true);

    }

    public boolean getPaused() {
        return Paused;
    }

    public void setPaused(boolean pauseState) {
        this.Paused = pauseState;
    }

    public void bildirimEkle(String str) {
        bildirimpaneli.setText(str);
        new Timer(3000, e -> bildirimpaneli.setText("")).start();
    }

    public void silahUpdate() {
        String bildirim = oyuncu.silah.getName();
        int mermi = oyuncu.silah.mermi;
        silahpaneli.setText("Silah:" + bildirim + "\n Mermi:" + mermi);
    }

    public void canUpdate() {
        int bildirim = oyuncu.can;
        canpaneli.setText("Can:" + bildirim);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!Paused) {
            setFocusable(true);
            requestFocusInWindow();

            for (Zombiler z : zombiler) {
                z.moveTowards(oyuncu.x, oyuncu.y);
            }

            for (Zombiler zombi : zombiler) {
                zombi.Attack(oyuncu, oyuncu.x, oyuncu.y);
                if (oyuncu.can == 0) {
                    bildirimpaneli.setText("GAME OVER!");
                    Paused = true;
                    timer.stop();
                }
            }

            for (int i = 0; i < mermiler.size(); i++) {
                Mermi mermi = mermiler.get(i);
                mermi.move();
                mermi.check(zombiler, oyuncu);
                if (mermi.isDead()) {
                    mermiler.remove(i);
                    i--;
                }
            }
            // mermiler.removeIf(mermi -> !mermi.alive());

            int dead = 0;
            for (Zombiler z : zombiler) {
                if (z.isDead()) {
                    dead++;
                }
            }
            if (dead == zombiler.size()) {
                wave++;
                if (wave == 2) {
                    PiyadeTüfeği piyadeTüfeği1 = new PiyadeTüfeği();
                    oyuncu.envanter.addItem(1, piyadeTüfeği1);
                    bildirimEkle("Yeni silah edinildi: Piyade Tüfeği");
                    bildirimEkle("Kuşanmak için 2 tuşuna basın.");
                }
                if (wave == 4) {
                    PompaliTüfek pompaliTüfek1 = new PompaliTüfek();
                    oyuncu.envanter.addItem(2, pompaliTüfek1);
                    bildirimEkle("Yeni silah edinildi: Pompalı Tüfek");
                    bildirimEkle("Kuşanmak için 2 tuşuna basın.");
                }
                if (wave == 6) {
                    KeskinNişanciTüfeği keskinNişanciTüfeği1 = new KeskinNişanciTüfeği();
                    oyuncu.envanter.addItem(3, keskinNişanciTüfeği1);
                    bildirimEkle("Yeni silah edinildi: Keskin Nişancı Tüfeği");
                    bildirimEkle("Kuşanmak için 2 tuşuna basın.");
                }
                if (wave == 11) {
                    RoketAtar roketAtar = new RoketAtar();
                    oyuncu.envanter.addItem(4, roketAtar);
                    bildirimEkle("Yeni silah edinildi: Roket Atar");
                    bildirimEkle("Kuşanmak için 2 tuşuna basın.");
                }
                bildirimpaneli.setText("Wave bitti!");
                bildirimpaneli.setText("Yeni wave geliyor...");
                zombiSpawnla();
            }

            zombiler.removeIf(zombi -> {
                if (zombi.isDead()) {
                    lootDüşür(zombi);
                    return true;
                }
                return false;
            });

            repaint();
            canUpdate();
        }

    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        oyuncu.draw(g);

        for (Mermi mermi : mermiler) {
            if (mermi.isalive() == true)
                mermi.draw(g);
        }

        for (Zombiler zombi : zombiler) {
            zombi.draw(g);
        }

        for (PickUpAble loot : loots) {
            if (loots instanceof Şarjör) {
                Şarjör şarjör = (Şarjör) loot;
                şarjör.draw(getGraphics());
            }
        }

    }

    public void zombiSpawnla() {
        if (difficulty == 0) {
            easyZombieMod();
        } else if (difficulty == 1) {
            normalZombieMod();
        } else {
            hardZombieMod();
        }
    }

    public void easyZombieMod() {
        Random random = new Random();
        for (int i = 0; i < wave * 1; i++) {
            int x = random.nextInt(700);
            int y = random.nextInt(500);
            int tür = random.nextInt(2);
            switch (tür) {
                case 0:
                    zombiler.add(new NormalZombi(x, y));
                    break;
                case 1:
                    zombiler.add(new SürüngeZombi(x, y));
                    break;
            }
        }

    }

    public void normalZombieMod() {
        Random random = new Random();
        for (int i = 0; i < wave * 2; i++) {
            int x = random.nextInt(800);
            int y = random.nextInt(600);
            int tür = random.nextInt(3);
            switch (tür) {
                case 0:
                    zombiler.add(new NormalZombi(x, y));
                    break;
                case 1:
                    zombiler.add(new SürüngeZombi(x, y));
                    break;
                case 2:
                    zombiler.add(new TankZombi(x, y));
                    break;
            }
        }
    }

    public void hardZombieMod() {
        Random random = new Random();
        for (int i = 0; i < wave * 3; i++) {
            int x = random.nextInt(800);
            int y = random.nextInt(600);
            int tür = random.nextInt(4);

            switch (tür) {
                case 0:
                    zombiler.add(new NormalZombi(x, y));
                    break;
                case 1:
                    zombiler.add(new SürüngeZombi(x, y));
                    break;
                case 2:
                    zombiler.add(new TankZombi(x, y));
                    break;
                case 3:
                    zombiler.add(new AsitTükürenZombi(x, y));
                    break;

            }
        }
    }

    public void lootDüşür(Zombiler zombi) {
        Random random = new Random();
        int dropchance = random.nextInt(100);

        if (dropchance < 50) {
            int lootchance = random.nextInt(4);
            switch (lootchance) {
                case 0:
                    loots.add(new Şarjör(new Tabanca(), zombi.x, zombi.y));
                    System.out.println("Loot düştü!");
                    break;
                case 1:
                    loots.add(new Şarjör(new PiyadeTüfeği(), zombi.x, zombi.y));
                    System.out.println("Loot düştü!");
                    break;
                case 2:
                    loots.add(new Şarjör(new PompaliTüfek(), zombi.x, zombi.y));
                    System.out.println("Loot düştü!");
                    break;
                case 3:
                    loots.add(new Şarjör(new KeskinNişanciTüfeği(), zombi.x, zombi.y));
                    System.out.println("Loot düştü!");
                    break;
                case 4:
                    loots.add(new Şarjör(new RoketAtar(), zombi.x, zombi.y));
                    System.out.println("Loot düştü!");
                    break;
            }

        }
    }

    public void saveGame(String file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("save.txt"))) {
            writer.write(oyuncu.getX() + "," + oyuncu.getY() + "," + oyuncu.getHealth());
            writer.newLine();
            for (Items item : oyuncu.getEnvanter()) {
                if (item instanceof Tabanca) {
                    Tabanca silah = (Tabanca) item;
                    writer.write("Tabanca" + silah.mermi + "," + silah.Şarjör);
                } else if (item instanceof PiyadeTüfeği) {
                    PiyadeTüfeği silah = (PiyadeTüfeği) item;
                    writer.write("PiyadeTüfeği" + silah.mermi + "," + silah.Şarjör);
                } else if (item instanceof PompaliTüfek) {
                    PompaliTüfek silah = (PompaliTüfek) item;
                    writer.write("PompaliTüfek" + silah.mermi + "," + silah.Şarjör);
                } else if (item instanceof KeskinNişanciTüfeği) {
                    KeskinNişanciTüfeği silah = (KeskinNişanciTüfeği) item;
                    writer.write("KeskinNişanciTüfeği" + silah.mermi + "," + silah.Şarjör);
                } else if (item instanceof RoketAtar) {
                    RoketAtar silah = (RoketAtar) item;
                    writer.write("RoketAtar" + silah.mermi + "," + silah.Şarjör);
                } else {
                    writer.write(item.getName());
                }
                writer.newLine();
            }

        } catch (IOException I) {
            I.printStackTrace();
            JOptionPane.showMessageDialog(null, "Kaydetme Başarısız!", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadGame(String file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String oyuncuData = reader.readLine();
            String[] değerler = oyuncuData.split(",");
            oyuncu.setX(Integer.parseInt(değerler[1]));
            oyuncu.setY(Integer.parseInt(değerler[2]));
            oyuncu.setHealth(Integer.parseInt(değerler[3]));

            String line = "";
            String[] silahdata = line.split(",");
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Tabanca")) {
                    Tabanca silah = (Tabanca) oyuncu.envanter.get(0);
                    silah.mermi = (Integer.parseInt(silahdata[1]));
                    silah.Şarjör = (Integer.parseInt(silahdata[2]));

                } else if (line.startsWith("PiyadeTüfeği")) {
                    PiyadeTüfeği silah = new PiyadeTüfeği();
                    silah.mermi = (Integer.parseInt(silahdata[1]));
                    silah.Şarjör = (Integer.parseInt(silahdata[2]));
                    oyuncu.envanter.addItem(1, silah);
                } else if (line.startsWith("PompaliTüfek")) {
                    PompaliTüfek silah = new PompaliTüfek();
                    silah.mermi = (Integer.parseInt(silahdata[1]));
                    silah.Şarjör = (Integer.parseInt(silahdata[2]));
                    oyuncu.envanter.addItem(2, silah);
                } else if (line.startsWith("KeskinNişanciTüfeği")) {
                    KeskinNişanciTüfeği silah = new KeskinNişanciTüfeği();
                    silah.mermi = (Integer.parseInt(silahdata[1]));
                    silah.Şarjör = (Integer.parseInt(silahdata[2]));
                    oyuncu.envanter.addItem(3, silah);
                } else if (line.startsWith("RoketAtar")) {
                    RoketAtar silah = new RoketAtar();
                    silah.mermi = (Integer.parseInt(silahdata[1]));
                    silah.Şarjör = (Integer.parseInt(silahdata[2]));
                    oyuncu.envanter.addItem(4, silah);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Yükleme Başarısız!", "Hata", JOptionPane.ERROR_MESSAGE);
        }

    }

    public void setDiff(int difficulty) {
        this.difficulty = difficulty;
    }

}

class Menü extends JPanel {

    Menü() {
        setSize(800, 600);
        setLayout(new GridLayout(3, 1));
        setBackground(Color.GREEN);
    }

}

class Difficulty extends JPanel {

    Difficulty(GamePanel game) {

        setSize(800, 600);
        setBackground(Color.GREEN);
        setLayout(new GridLayout(4, 1));

        JLabel choose = new JLabel("Zorluk Ayarını Seçiniz", SwingConstants.CENTER);
        add(choose);

        JButton easy = new JButton("Kolay");
        easy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                game.setDiff(1);
                setVisible(false);
                game.setPaused(false);
                game.setVisible(true);
            }
        });
        add(easy);

        JButton normal = new JButton("Normal");
        normal.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent n) {
                game.setDiff(2);
                setVisible(false);
                game.setPaused(false);
                game.setVisible(true);
            }
        });
        add(normal);

        JButton hard = new JButton("Zor");
        hard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent h) {
                game.setDiff(3);
                setVisible(false);
                game.setPaused(false);
                game.setVisible(true);
            }
        });
        add(hard);

    }

}

class Harita {

    private BufferedImage harita;

    public Harita(String dosya) {
        try {
            harita = ImageIO.read(new File(dosya));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics g) {
        g.drawImage(harita, 0, 0, null);
    }

}

class Oyuncu {

    protected int can;
    protected int hiz;
    protected Silahlar silah;
    protected int x, y;
    protected BufferedImage oyuncupng;
    protected Envanter<Items> envanter;

    Oyuncu(int x, int y, Silahlar silah) {
        this.can = 100;
        this.hiz = 2;
        this.silah = silah;
        this.x = x;
        this.y = y;
        this.envanter = new Envanter();

        try {
            oyuncupng = ImageIO.read(getClass().getResource("/resource/oyuncu1.png"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Oyuncu resmi yüklenemedi!");
            oyuncupng = null;
        }

    }

    public void draw(Graphics g) {
        if (oyuncupng != null) {
            g.drawImage(oyuncupng, x, y, null);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(x, y, 32, 32);
        }

    }

    public void pozDeğiştir() {

    }

    public void move(int newX, int newY) {
        x += hiz * newX;
        y += hiz * newY;
    }

    public void takeDamage(int hasar) {
        can -= hasar;

    }

    public void ates(List<Zombiler> zombiler, int mouseX, int mouseY) {
        silah.fire(zombiler, x, y, mouseX, mouseY);
    }

    public void silahReload() {
        silah.reload();
    }

    public void silahDeğiş(Silahlar silah) {
        this.silah = silah;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 32, 32);
    }

    public Envanter<Items> getEnvanter() {
        return envanter;
    }

    public void setEnvanter(Envanter<Items> yeniEnvanter) {
        this.envanter = yeniEnvanter;
    }

    public int getHealth() {
        return can;
    }

    public int getAmmo() {
        return silah.mermi;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setHealth(int yenican) {
        this.can = yenican;
    }

    public void setX(int X) {
        this.x = X;
    }

    public void setY(int Y) {
        this.y = Y;
    }

}

class Envanter<T extends Items> extends ArrayList<Items> {
    public ArrayList<Items> envanter;

    Envanter() {
        envanter = new ArrayList<Items>();
    }

    public void addItem(int index, Items item) {
        envanter.add(index, item);
        System.out.println(item.getName() + " envantere eklendi!");
    }

    public void addItem2(Items item) {
        envanter.add(item);
        System.out.println(item.getName() + " envantere eklendi!");
    }

    public int foundItem(Items item) {
        for (Items allitems : envanter) {
            if (allitems.getName().equals(item.getName())) {
                return envanter.indexOf(allitems);
            }
        }
        return -1;
    }

    public void removeItem(int index, Items item) {
        envanter.remove(item);
        System.out.println("Item removed");
    }

}

interface Items {

    public String getName();

}

interface PickUpAble {
    public void pickUp(Oyuncu oyuncu);
}

abstract class Silahlar implements Items {
    protected int kapasite;
    protected int hiz;
    protected int mermi;
    protected int menzil;
    protected int hasar;
    protected List<Mermi> mermiler;
    protected int Şarjör;

    Silahlar(int kapasite, int hiz, int menzil, int hasar) {
        this.kapasite = kapasite;
        this.hiz = hiz;
        this.mermi = kapasite;
        this.menzil = menzil;
        this.hasar = hasar;
        mermiler = new ArrayList<>(kapasite);
        this.Şarjör = 0;
    }

    public void fire(List<Zombiler> zombieler, int oyuncuX, int oyuncuY, int mouseX, int mouseY) {
        if (mermi > 0) {
            mermi--;
            Mermi realmermi = new Mermi(oyuncuX, oyuncuY, mouseX, mouseY, hiz);
            mermiler.add(realmermi);

        } else {
            System.out.println("Mermi bitti!");
        }

    }

    public void reload() {
        if (Şarjör > 0) {
            mermi = kapasite;
            Şarjör--;
        } else {
            System.out.println("Yedek şarjörün yok!");
        }
    }

    public void addMermi(int yenimermi) {
        mermi += yenimermi;
    }

    public void addŞarjör() {
        Şarjör++;
    }

    public void mermiUpdate() {
        mermiler.clear();
    }

    public int getAmmo() {
        return mermi;
    }

    public int getKapasite() {
        return kapasite;
    }

    public String getName() {
        return "Silah";
    }

}

class Tabanca extends Silahlar {

    Tabanca() {
        super(12, 120, 50, 10);
    }

    @Override
    public void reload() {
        mermi = kapasite;
    }

    public String getName() {
        return "Tabanca";
    }
}

class PiyadeTüfeği extends Silahlar {

    PiyadeTüfeği() {
        super(30, 600, 50, 20);
    }

    @Override
    public void fire(List<Zombiler> zombieler, int oyuncuX, int oyuncuY, int mouseX, int mouseY) {
        if (mermi > 0) {
            mermi--;
            double sapma = 30.0;
            double açi = (Math.random() * (2 + sapma)) - sapma;
            double hedef = Math.atan2(mouseY - oyuncuY, mouseX - oyuncuX);
            double sapmişaçi = hedef + Math.toRadians(açi);
            double sMouseX = oyuncuX + Math.cos(sapmişaçi) * 100;
            double sMouseY = oyuncuY + Math.sin(sapmişaçi) * 100;

            Mermi mermi = new Mermi(oyuncuX, oyuncuY, (int) sMouseX, (int) sMouseY, hiz);
            mermiler.add(mermi);
        } else {
            System.out.println("Mermi bitti!");
        }
    }

    public String getName() {
        return "Piyade Tüfeği";
    }
}

class PompaliTüfek extends Silahlar {

    PompaliTüfek() {
        super(5, 60, 50, 25);
    }

    @Override
    public void fire(List<Zombiler> zombieler, int oyuncuX, int oyuncuY, int mouseX, int mouseY) {
        if (mermi > 0) {
            mermi -= 9;
            double center = Math.atan2(mouseY - oyuncuY, mouseX - oyuncuX);
            double max = Math.toRadians(22.5);
            double fark = Math.toRadians(5);

            for (int i = -4; i <= 4; i++) {
                double angle = center + (i * fark);
                int x = (int) (Math.cos(angle) * hiz);
                int y = (int) (Math.sin(angle) * hiz);
                mermiler.add(new Mermi(oyuncuX, oyuncuY, oyuncuX + x, oyuncuY + y, hiz));
            }

        } else {
            System.out.println("Mermi bitti!");
        }
    }

    public String getName() {
        return "Pompali Tüfek";
    }
}

class KeskinNişanciTüfeği extends Silahlar {

    KeskinNişanciTüfeği() {
        super(5, 30, 100, 80);
    }

    public String getName() {
        return "Keskin Nişanci Tüfeği";
    }
}

class RoketAtar extends Silahlar {

    RoketAtar() {
        super(1, 10, 40, 100);
    }

    public String getName() {
        return "Roket Atar";
    }
}

class Şarjör implements Items, PickUpAble {

    protected int mermikapasite;
    protected boolean used;
    protected Silahlar silah;
    protected int x;
    protected int y;
    protected BufferedImage şarjörpng;

    Şarjör(Silahlar kullanilansilah, int x, int y) {
        this.x = x;
        this.y = y;
        this.mermikapasite = kullanilansilah.getKapasite();
        this.used = false;
        this.silah = kullanilansilah;
        try {
            şarjörpng = ImageIO.read(getClass().getResource("/resource/şarjör.png"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Şarjör resmi yüklenemedi!");
            şarjörpng = null;
        }
    }

    public void pickUp(Oyuncu oyuncu) {
        if (Math.abs(x - oyuncu.getX()) < 5 && Math.abs(y - oyuncu.getY()) < 5) {
            oyuncu.silah.addŞarjör();
        }
    }

    public void draw(Graphics g) {
        if (şarjörpng != null) {
            g.drawImage(şarjörpng, x, y, null);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(x, y, 16, 16);
        }
    }

    public void use() {
        if (!used) {
            silah.addMermi(mermikapasite);
            used = true;
        }
    }

    public String getName() {
        return "Şarjör";
    }
}

class Mermi {

    protected int x;
    protected int y;
    protected int mouseX;
    protected int mouseY;
    protected int hiz;
    protected boolean alive;

    protected final double angle;
    private final double m;
    private final double b;

    public Mermi(int oyuncux, int oyuncuy, int mouseX, int mouseY, int hiz) {
        this.x = oyuncux;
        this.y = oyuncuy;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.hiz = hiz;
        this.alive = true;
        angle = Math.atan2(mouseY - y, mouseX - x);
        m = (mouseY - y) / (mouseX - x);
        b = y - m * x;
    }

    public void draw(Graphics g) {
        if (alive) {
            g.setColor(Color.BLACK);
            g.fillOval(x - 2, y - 2, 6, 6);
        }
    }

    public void move() {
        if (!alive)
            return;

        x += Math.cos(angle) * hiz;
        y += Math.sin(angle) * hiz;

        if (x < 0 || x > 800 || y < 0 || y > 600) {
            alive = false;
        }
    }

    public void check(List<Zombiler> zombiler, Oyuncu oyuncu) {
        for (Zombiler zombi : zombiler) {
            if (this.getBounds().intersects(zombi.box())) {
                alive = false;
                zombi.takeDamage(oyuncu.silah.hasar);
                System.out.println("Zombi hasar aldı");
                break;
            }
        }
    }

    public boolean lineCheck(List<Zombiler> zombiler) {
        for (Zombiler zombi : zombiler) {
            Rectangle box = zombi.getBounds();

            for (int xx = box.x; x <= box.x + box.width; xx++) {
                int yy = (int) Math.round(m * x + b);
                if (box.contains(xx, yy)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isDead() {
        return !alive;
    }

    public boolean isalive() {
        return alive;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 6, 6);
    }

}

abstract class Zombiler {
    protected int can;
    protected int hiz;
    protected int hasar;
    protected int x, y;
    protected BufferedImage zombipng;
    private long sonsaldiri;
    private final long cooldown;

    Zombiler(int x, int y, int can, int hiz, int hasar) {
        this.can = can;
        this.hiz = hiz;
        this.hasar = hasar;
        this.x = x;
        this.y = y;
        this.sonsaldiri = 0;
        this.cooldown = 2000;
    }

    public Rectangle box() {
        return new Rectangle(x, y, 32, 32);
    }

    public void takeDamage(int hasar) {
        can -= hasar;
    }

    public boolean isDead() {
        if (can <= 0) {
            return true;
        }
        return false;
    }

    public void moveTowards(int playerX, int playerY) {

        int dx = playerX - x;
        int dy = playerY - y;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length != 0) {
            x += (int) (hiz * dx / length);
            y += (int) (hiz * dy / length);
        }

    }

    public void Attack(Oyuncu oyuncu, int oyuncuX, int oyuncuY) {

        long şimdi = System.currentTimeMillis();

        if (şimdi - sonsaldiri >= cooldown) {
            if (this.getBounds().intersects(oyuncu.getBounds())) {
                oyuncu.takeDamage(hasar);
                sonsaldiri = şimdi;
                System.out.println("Hasar verildi!");
            }
        }

    }

    public void draw(Graphics g) {
        if (zombipng != null) {
            g.drawImage(zombipng, x, y, null);
        } else {
            g.setColor(Color.GREEN);
            g.fillRect(x, y, 32, 32);
        }

    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 32, 32);
    }

}

// Can = Yüksek: 100, Orta: 50; Düşük: 25
// Hız = Hızlı: 3, Yavaş: 2, Çok Yavaş: 1
// Hasar = Yüksek: 20, Orta: 10, Düşük: 5

class NormalZombi extends Zombiler {
    protected BufferedImage zombipng;

    NormalZombi(int x, int y) {
        super(x, y, 50, 1, 10);
        try {
            zombipng = ImageIO.read(getClass().getResource("/resource/normalzombi.png"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Zombi resmi yüklenemedi!");
            zombipng = null;
        }
    }

    public void draw(Graphics g) {
        if (zombipng != null) {
            g.drawImage(zombipng, x, y, null);
        } else {
            g.setColor(Color.GREEN);
            g.fillRect(x, y, 32, 32);
        }

    }

}

class SürüngeZombi extends Zombiler {
    protected BufferedImage zombipng;

    SürüngeZombi(int x, int y) {
        super(x, y, 25, 3, 10);

        try {
            zombipng = ImageIO.read(getClass().getResource("/resource/sürüngezombi.png"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Zombi resmi yüklenemedi!");
            zombipng = null;
        }
    }

    @Override
    public void moveTowards(int oyuncux, int oyuncuy) {

        int dx = oyuncux - x;
        int dy = oyuncuy - y;
        double mesafe = Math.sqrt(dx * dx + dy * dy);

        if (mesafe < 50) {
            int zhiz = 10;
            if (mesafe != 0) {
                x += (int) (zhiz * dx / mesafe);
                y += (int) (zhiz * dy / mesafe);
            }
        } else {
            if (mesafe != 0) {
                x += (int) (hiz * dx / mesafe);
                y += (int) (hiz * dy / mesafe);
            }
        }

    }

    public void draw(Graphics g) {
        if (zombipng != null) {
            g.drawImage(zombipng, x, y, null);
        } else {
            g.setColor(Color.RED);
            g.fillRect(x, y, 32, 32);
        }

    }
}

class TankZombi extends Zombiler {
    protected BufferedImage zombipng;

    TankZombi(int x, int y) {
        super(x, y, 100, 1, 20);
        try {
            zombipng = ImageIO.read(getClass().getResource("/resource/tankzombi.png"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Zombi resmi yüklenemedi!");
            zombipng = null;
        }
    }

    public void draw(Graphics g) {
        if (zombipng != null) {
            g.drawImage(zombipng, x, y, null);
        } else {
            g.setColor(Color.GRAY);
            g.fillRect(x, y, 32, 32);
        }

    }

}

class AsitTükürenZombi extends Zombiler {
    protected BufferedImage zombipng;

    AsitTükürenZombi(int x, int y) {
        super(x, y, 25, 2, 10);
        try {
            zombipng = ImageIO.read(getClass().getResource("/resource/asittükürenzombi.png"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Zombi resmi yüklenemedi!");
            zombipng = null;
        }
    }

    public void draw(Graphics g) {
        if (zombipng != null) {
            g.drawImage(zombipng, x, y, null);
        } else {
            g.setColor(Color.CYAN);
            g.fillRect(x, y, 32, 32);
        }

    }
}
