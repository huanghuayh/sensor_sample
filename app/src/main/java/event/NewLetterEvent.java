package event;

/**
 * Created by harry on 12/5/16.
 */
public class NewLetterEvent {

    private String current_letter;
    public NewLetterEvent(String letter){
        current_letter=letter;
    }


    public String get_current_letter(){
        return current_letter;
    }
}
