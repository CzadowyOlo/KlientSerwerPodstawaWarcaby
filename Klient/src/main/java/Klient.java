import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Scanner;
import java.io.PrintWriter;
import java.net.Socket;

public class Klient {

    private Socket socket;
    private Scanner in;
    private PrintWriter out;
    public Ramka frame;
    public boolean tura = false; //informacja od serwera, czy dany gracz ma teraz swoją turę
    final MusicClient mp3 = new MusicClient();
    boolean raz = true;

/////////////////////////////////////////////////////
    public ActionListener wyb_pionek = new ActionListener() {

        public boolean wybrano_piona = true; // pomaga określić czy trzeba wybrać pionka czy ruszyć pionka
        // true -> kliknięcie pola_planszy wybiera pionka którego chcemy ruszyć
        // false -> kilknięcie pola_planszy stawia wcześniej wybranego piona na wybrane miejsce
        Color kolor_piona;
        int currentX;
        int currentY;
        int previousX;
        int previousY;
        final Enigma enigma = new Enigma();

        @Override
        public void actionPerformed(ActionEvent e) { //obsługa ruchu

            String coordinates = ((JComponent) e.getSource()).getName();
            System.out.println(coordinates);
            currentX = frame.panelGry.get_current_X(coordinates);
            currentY = frame.panelGry.get_current_Y(coordinates);

            if(wybrano_piona){


                if(frame.panelGry.pola_planszy[currentX][currentY].getBackground() != Color.WHITE){

                    mp3.playSound("markpiona.wav");
                    kolor_piona = frame.panelGry.pola_planszy[currentX][currentY].getBackground();
                    frame.panelGry.check_ALL(currentX, currentY);
                    previousX = currentX;
                    previousY = currentY;

                    wybrano_piona = false;

                    System.out.println();
                }
                else{
                    System.out.println("Wybierz kolorowe pole ");
                }
            }
            else{
                if(frame.panelGry.pola_planszy[currentX][currentY].getBackground() == Color.GRAY || frame.panelGry.pola_planszy[currentX][currentY].getBackground() == Color.WHITE) {

                    //|| frame.panelGry.pola_planszy[currentX][currentY].getBackground() == Color.WHITE //god mode

                    System.out.println("Teraz nalezy wybrac gdzie sie ruszyc");
                    frame.panelGry.clear_grey();

                    if(tura){
                        mp3.playSound("koniecruchu.wav");
                        out.println("MOVE" + previousX + "," + previousY + "," + currentX + "," + currentY + "," + enigma.koduj_kolor(kolor_piona));
                        tura = false;
                    }

                    wybrano_piona = true;
                }
                else if (previousX == currentX && previousY == currentY){
                    frame.panelGry.clear_grey();
                    wybrano_piona = true;
                }
                else{
                    System.out.println("zle pole");
                }
            }
        }
    };

    /**
     * obsługa pominięcia kolejki
     */
    public ActionListener skiper = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(tura){
                out.println("SKIP");
                tura = false;
            }
        }
    };

    //////////////////////////////////////////
    public Klient(String serverAddress) throws Exception {
        socket = new Socket(serverAddress, 58901);
        in = new Scanner(socket.getInputStream());
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void play() throws Exception {
        try {
            var response = in.nextLine();
            var num = response.charAt(9);
            var pom = num;
            Enigma enigma2 = new Enigma();
            var ilosc = Character.getNumericValue(response.charAt(0));
            char kolorgracza = enigma2.idgracza(num, ilosc);
            System.out.println("Witaj graczu o numerze: " + num + " " + kolorgracza);
            System.out.println("ilość graczy wynosi: " + ilosc);

            frame = new Ramka(ilosc, num, enigma2.kolorgracza(num, ilosc), enigma2.set_desktop_x(num), enigma2.set_desktop_y(num));
            frame.panelGry.dodaj_wlasciwosci_guzikom(wyb_pionek);
            frame.pass.addActionListener(skiper);
            frame.setVisible(true);

            while (in.hasNextLine()) {
                response = in.nextLine();

                if(response.startsWith("MESSAGE")){
                    System.out.println(response);

                    if(response.charAt(15) == num){
                        tura = true;
                    }
                }

                else if(response.startsWith("TURN")){

                    System.out.println("teraz jest tura gracza o numerze: " + response.charAt(4));
                    frame.which_player.setBackground(enigma2.kolorgracza(response.charAt(4), ilosc));
                    if(response.charAt(4) == num){
                        tura = true;
                        System.out.println("twoja tura");
                        if(frame.panelGry.wygrana(kolorgracza)){
                            System.out.println("KONIEC!");
                            out.println("SKIP");
                        }
                    }
                }
                else if(response.startsWith("MOVE")){ //serwer wysłał wiadomość o ruchu jakiegoś gracza
                    System.out.println(response); //musimy skopiować ten ruch u nas

                    enigma2.koloruj(response, frame);// oddtworzenie ruchu gracza u nas
                    if(frame.panelGry.wygrana(kolorgracza) && raz){
                        System.out.println("KONIEC! WYGRALES");
                        mp3.playSound("epicwin.wav");
                        out.println("WINNER" + pom);
                        frame.setVisible(false);
                        raz = false;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("koniec");
            //socket.close();
            //frame.dispose();
        }
    }

    /**
     * uruchomienie klienta
     * @param args ipv4
     * @throws Exception ex
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        Klient client = new Klient(args[0]);
        client.play();
    }
}
