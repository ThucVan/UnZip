package com.thaiduong.unzip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hwangjr.rxbus.RxBus;

import me.pengtao.filetransfer.Constants;

/**
 * @author CPPAlien
 */
public class PackageStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, 0);
    }
}
