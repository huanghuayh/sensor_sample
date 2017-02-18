package jingzhong1233.offlinecollect;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import event.BusProvider;
import event.NewLetterEvent;
import event.RecordFinishEvent;

/**
 * Created by harry on 12/5/16.
 */
public class LetterCounter {

    String TAG="/LetterCounter/";
    int WRITING_TIME=6000;
    int START_TIME=4000;
    String[] letter_lst;
    int cur_letter_index;

    public LetterCounter(String[] input_letter_lst){
        this.letter_lst=input_letter_lst;
        this.cur_letter_index=0;
    }



    public Timer m_repeating_timer;
    public void start_train_timer(){
        if(m_repeating_timer!=null){
            m_repeating_timer.cancel();
            m_repeating_timer=null;
        }
        m_repeating_timer=new Timer();
        m_repeating_timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(cur_letter_index>=letter_lst.length){
                    BusProvider.postOnMainThread(new RecordFinishEvent());

                    cancel();
                    return;

                }
                Log.d(TAG,"repeating timer");
                BusProvider.postOnMainThread(new NewLetterEvent(letter_lst[cur_letter_index]));
                cur_letter_index++;



            }
        }, START_TIME, WRITING_TIME);
    }

    public void stop_train_timer(){

        Log.d(TAG,"try to terminate counter");
        if(m_repeating_timer!=null){

            BusProvider.postOnMainThread(new RecordFinishEvent());
            m_repeating_timer.cancel();
            m_repeating_timer=null;
        }
    }
}
