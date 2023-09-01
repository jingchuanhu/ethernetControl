package com.minrray.ethernet;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FragmentManager fragmentManager = getSupportFragmentManager();

        //步骤一：添加一个FragmentTransaction的实例

        //beginTransaction()
        //Start a series of edit operations on the Fragments associated with this FragmentManager.
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        //步骤二：用add()方法加上Fragment的对象rightFragment
        EthernetFragment rightFragment = new EthernetFragment();
        transaction.add(R.id.fragment_layout, rightFragment);

        //步骤三：调用commit()方法使得FragmentTransaction实例的改变生效
        transaction.commit();
    }
}
