/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package com.github.ucchyocean.chatbot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * タイマータスク
 * @author ucchy
 */
public class TimerTask extends BukkitRunnable {

    private static final String SIGNAL_FORMAT = "HHmm";
    private static final String ALERM_FORMAT = "MMddHHmm";
    private static final String REPEAT_REGEX = "R([0-9]{1,4})";

    // 最後に時報を行った時刻の文字列
    private String lastSignal;

    private SimpleDateFormat time_format;
    private SimpleDateFormat date_format;
    private Pattern repeat_pattern;
    private ChatBotConfig config;
    private TimeSignalData signalData;

    private ArrayList<BukkitRunnable> notifies;
    private KeywordReplacer replacer;

    /**
     * コンストラクタ
     * @param config コンフィグ
     * @param signalData 時報データ
     */
    public TimerTask(ChatBotConfig config, TimeSignalData signalData) {

        this.config = config;
        this.signalData = signalData;
        this.replacer = new KeywordReplacer();

        time_format = new SimpleDateFormat(SIGNAL_FORMAT);
        date_format = new SimpleDateFormat(ALERM_FORMAT);
        repeat_pattern = Pattern.compile(REPEAT_REGEX);

        // 繰り返し通知を起動する。
        if ( config.isRepeatSignals() ) {

            notifies = new ArrayList<BukkitRunnable>();

            for ( String key : signalData.getAllKeys() ) {

                Matcher matcher = repeat_pattern.matcher(key);

                if ( matcher.matches() ) {

                    int minutes = Integer.parseInt(matcher.group(1));
                    if ( minutes == 0 ) continue;
                    int ticks = minutes * 60 * 20;
                    final String responce = signalData.getResponceIfMatch(key);

                    BukkitRunnable notify =
                        new BukkitRunnable() {
                            public void run() {
                                MintChatBot.getInstance().say(
                                        replacer.replace(responce, null, null, null, null));
                            }
                        };
                    notify.runTaskTimerAsynchronously(MintChatBot.getInstance(), ticks, ticks);
                    notifies.add(notify);
                }
            }
        }
    }

    /**
     * タスクが動作する時に呼び出されるメソッド
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        Date date = new Date();
        String time = time_format.format(date);

        if ( time.equals(lastSignal) ) {
            return;
        }

        if ( config.isTimeSignals() ) {

            // 時報の処理
            String responce = signalData.getResponceIfMatch(time);
            MintChatBot.getInstance().say(
                    replacer.replace(responce, null, null, null, null));
        }

        if ( config.isAlermSignals() ) {

            // アラームの処理
            String datetime = date_format.format(date);
            String responce = signalData.getResponceIfMatch(datetime);
            MintChatBot.getInstance().say(
                    replacer.replace(responce, null, null, null, null));
        }

        lastSignal = time;
    }

    /**
     * 全ての繰り返し通知をキャンセルする
     */
    protected void cancelAllNotifies() {
        if ( notifies == null ) return;
        for ( BukkitRunnable r : notifies ) {
            r.cancel();
        }
    }
}
