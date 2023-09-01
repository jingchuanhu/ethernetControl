package com.minrray.ethernet;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.InetAddresses;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.StaticIpConfiguration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EthernetFragment extends Fragment implements TextWatcher {

    private View view;

    private EthernetManager mEthManager = null;

    private final String TAG = EthernetFragment.class.getSimpleName();

    private Context mContext;

    public enum ETHERNET_STATE {
        ETHER_STATE_DISCONNECTED,
        ETHER_STATE_CONNECTING,
        ETHER_STATE_CONNECTED
    }

    private final static String mNullIpInfo = "0.0.0.0";

    private static String mEthIpAddress = mNullIpInfo;

    private static String mEthNetmask = mNullIpInfo;

    private static String mEthGateway = mNullIpInfo;

    private static String mEthdns1 = mNullIpInfo;

    private static String mEthdns2 = mNullIpInfo;

    EditText mEthernetAddressEdit;

    EditText mEthernetGateWayEdit;

    EditText mEthernetMaskEdit;

    EditText mEthernetDnsEdit;

    EditText mEthernetDns2Edit;

    private CheckBox mDhcpCheckBox;

    private CheckBox mStaticCheckBox;

    private CheckBox mEthernetEnableCheckBox;

    private Button mEthernetSubmit;

    private StaticIpConfiguration mStaticIpConfiguration;

    private IpConfiguration mIpConfiguration;
    private IntentFilter mIntentFilter;
    private ConnectivityManager mConnectivityManager;
    private String mInterfaceName;

    private boolean isConnect = false;

    private final static String ETHERNET_ACTION = "android.intent.action.ETHERNET_LINEKSTATE";

    private CompoundButton.OnCheckedChangeListener onCheckedChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!buttonView.isPressed()) {
                return;
            }
            switch (buttonView.getId()) {
                case R.id.auto_getip_cbx:
                    Log.d(TAG, "onCheckedChanged: dhcp");
                    if (mInterfaceName != null) {
                        if (isChecked) {
                            mStaticCheckBox.setChecked(false);
                            IpConfiguration ipConfiguration = new IpConfiguration();
                            ipConfiguration.setIpAssignment(IpAssignment.DHCP);
                            ipConfiguration.setProxySettings(IpConfiguration.ProxySettings.NONE);
                            mEthManager.setConfiguration(mInterfaceName, ipConfiguration);
                        } else {
                            IpConfiguration ipConfiguration = new IpConfiguration();
                            ipConfiguration.setIpAssignment(IpAssignment.UNASSIGNED);
                            ipConfiguration.setProxySettings(IpConfiguration.ProxySettings.NONE);
                            mEthManager.setConfiguration(mInterfaceName, ipConfiguration);
                        }
                    }

                    break;
                case R.id.getip_cbx:
                    Log.d(TAG, "onCheckedChanged: getip");
                    if (mInterfaceName != null) {
                        if (isChecked) {
                            mDhcpCheckBox.setChecked(false);
//                            setStaticIpConfiguration();
                            updatePreference(true);
                        } else {
                            IpConfiguration ipConfiguration = new IpConfiguration();
                            ipConfiguration.setIpAssignment(IpAssignment.UNASSIGNED);
                            ipConfiguration.setProxySettings(IpConfiguration.ProxySettings.NONE);
                            mEthManager.setConfiguration(mInterfaceName, ipConfiguration);
                        }
                    }
                    break;
                case R.id.ethernet_enable_cbx:
                    Log.d(TAG, "onCheckedChanged: ethernet_enable_cbx");
                    if (mInterfaceName != null) {
                        mEthManager.setEnabled(isChecked);
                    }
                    break;

            }
        }
    };

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            mEthIpAddress = mEthernetAddressEdit.getText().toString();
            mEthNetmask = mEthernetMaskEdit.getText().toString();
            mEthGateway = mEthernetGateWayEdit.getText().toString();
            mEthdns1 = mEthernetDnsEdit.getText().toString();
            mEthdns2 = mEthernetDns2Edit.getText().toString();

            if (setStaticIpConfiguration()) {
                handleEtherStateChange(ETHERNET_STATE.ETHER_STATE_CONNECTING);
                mEthManager.setConfiguration(mInterfaceName, mIpConfiguration);
            } else {
                Log.e(TAG, mIpConfiguration.toString());
            }
        }
    };


    public static Fragment newInstance() {
        return new EthernetFragment();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (null != info && ConnectivityManager.TYPE_ETHERNET == info.getType()) {
                    if (NetworkInfo.State.CONNECTED == info.getState()) {
                        handleEtherStateChange(ETHERNET_STATE.ETHER_STATE_CONNECTED);
                    } else if (NetworkInfo.State.DISCONNECTED == info.getState()) {
                        handleEtherStateChange(ETHERNET_STATE.ETHER_STATE_DISCONNECTED);
                    }
                }
                getNetWorkInfo();
            } else if (action.equals(ETHERNET_ACTION)) {
                String eth0state = intent.getStringExtra("eth0");
                Log.d(TAG, "eth0state: " + eth0state);
                if (!TextUtils.isEmpty(eth0state)) {
                    if ("up".equals(eth0state)) {
                        mEthernetEnableCheckBox.setChecked(true);
                        Toast.makeText(mContext, "网线已接上!", Toast.LENGTH_SHORT).show();
                        getEthInfo();
                    } else {
                        mEthernetEnableCheckBox.setChecked(false);
                        Toast.makeText(mContext, "网线已断开!", Toast.LENGTH_SHORT).show();
                        handleEtherStateChange(ETHERNET_STATE.ETHER_STATE_DISCONNECTED);
                    }
                }
            }
        }
    };

    private void getNetWorkInfo() {
        if (mConnectivityManager != null) {
            NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
            if (info != null) {
                Log.d(TAG, "getNetWorkInfo: 0");
                if (info.isAvailable()) {
                    Log.d(TAG, "getNetWorkInfo: " + info.getTypeName());
                    if ("Ethernet".equals(info.getTypeName())) {
                        Log.d(TAG, "getNetWorkInfo: " + info.getState());
                        if (info.getState() == NetworkInfo.State.CONNECTING) {
                            handleEtherStateChange(ETHERNET_STATE.ETHER_STATE_CONNECTING);
                        } else if (info.getState() == NetworkInfo.State.CONNECTED) {
                            handleEtherStateChange(ETHERNET_STATE.ETHER_STATE_CONNECTED);
                        } else {
                            handleEtherStateChange(ETHERNET_STATE.ETHER_STATE_DISCONNECTED);
                        }
                    }
                }

            }

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.ethernet_set_main, container,
                false);
        mEthernetAddressEdit = (EditText) view.findViewById(R.id.ethernet_set_address_edit);
        mEthernetGateWayEdit = (EditText) view.findViewById(R.id.ethernet_set_gateway_edit);
        mEthernetMaskEdit = (EditText) view.findViewById(R.id.ethernet_set_mask_edit);
        mEthernetDnsEdit = (EditText) view.findViewById(R.id.ethernet_set_dns_edit);
        mEthernetDns2Edit = (EditText) view.findViewById(R.id.ethernet_set_dns2_edit);

        mDhcpCheckBox = (CheckBox) view.findViewById(R.id.auto_getip_cbx);
        mStaticCheckBox = (CheckBox) view.findViewById(R.id.getip_cbx);
        mEthernetEnableCheckBox = (CheckBox) view.findViewById(R.id.ethernet_enable_cbx);
        mEthernetSubmit = (Button) view.findViewById(R.id.ethernet_set_submit);

        mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        mEthManager = (EthernetManager) getActivity().getSystemService(Context.ETHERNET_SERVICE);
        if (mEthManager == null) {
            Log.e(TAG, "get ethernet manager failed");
        }
        if (mConnectivityManager == null) {
            Log.e(TAG, "get connectivityManager failed");
        }
        mContext = this.getActivity().getApplicationContext();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ETHERNET_ACTION);
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        mEthernetAddressEdit.addTextChangedListener(this);
        mEthernetGateWayEdit.addTextChangedListener(this);
        mEthernetMaskEdit.addTextChangedListener(this);
        mEthernetDnsEdit.addTextChangedListener(this);
        mEthernetDns2Edit.addTextChangedListener(this);


        return view;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        checkIPValue();
    }


    @Override
    public void onResume() {
        super.onResume();
        getEthInfo();
        mDhcpCheckBox.setOnCheckedChangeListener(onCheckedChangedListener);
        mStaticCheckBox.setOnCheckedChangeListener(onCheckedChangedListener);
        boolean isEnable = mEthManager.isEnabled();
        Log.i(TAG, "isEnable: " + isEnable);
        mEthernetEnableCheckBox.setChecked(isEnable);
        mEthernetEnableCheckBox.setOnCheckedChangeListener(onCheckedChangedListener);
        mEthernetSubmit.setOnClickListener(onClickListener);
        getNetWorkInfo();
        mContext.registerReceiver(mReceiver, mIntentFilter);
        checkIPValue();
    }

    private void checkIPValue() {
        boolean enable = false;
        String ipAddr = mEthernetAddressEdit.getText().toString();
        String gateway = mEthernetGateWayEdit.getText().toString();
        String dns1 = mEthernetDnsEdit.getText().toString();
        String dns2 = mEthernetDns2Edit.getText().toString();
        String netMask = mEthernetMaskEdit.getText().toString();
        Pattern pattern = Pattern.compile("(^((\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])$)|^(\\d|[1-2]\\d|3[0-2])$"); /*check subnet mask*/
        if (isValidIpAddress(ipAddr) && isValidIpAddress(gateway)
                && isValidIpAddress(dns1) && (pattern.matcher(netMask).matches())) {
            if (TextUtils.isEmpty(dns2)) { // 为空可以不考虑
                enable = true;
            } else {
                if (isValidIpAddress(dns2)) {
                    enable = true;
                } else {
                    enable = false;
                }
            }
        } else {
            enable = false;
        }
        mEthernetSubmit.setEnabled(enable);
    }

    @Override
    public void onDestroy() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }


    private void handleEtherStateChange(ETHERNET_STATE EtherState) {
        Log.i(TAG, "curEtherState" + EtherState);

        switch (EtherState) {
            case ETHER_STATE_DISCONNECTED:
                mEthIpAddress = mNullIpInfo;
                mEthNetmask = mNullIpInfo;
                mEthGateway = mNullIpInfo;
                mEthdns1 = mNullIpInfo;
                mEthdns2 = mNullIpInfo;
                break;
            case ETHER_STATE_CONNECTING:
                String mStatusString = this.getResources().getString(R.string.ethernet_info_getting);
                mEthIpAddress = mStatusString;
                mEthNetmask = mStatusString;
                mEthGateway = mStatusString;
                mEthdns1 = mStatusString;
                mEthdns2 = mStatusString;
                break;
            case ETHER_STATE_CONNECTED:
                getEthInfo();
                break;
        }

        refreshUI();
    }


    public void getEthInfo() {
        String[] ifaces = mEthManager.getAvailableInterfaces();
        Log.i(TAG, "ifaces.len: " + ifaces.length);
        if (ifaces.length > 0) {
            mInterfaceName = ifaces[0];
            Log.i(TAG, "mInterfaceName: " + mInterfaceName);
            IpAssignment mode = mEthManager.getConfiguration(mInterfaceName).getIpAssignment();
            Log.i(TAG, "mode: " + mode);
            if (mode == IpAssignment.DHCP) {
                getEthInfoFromDhcp();
                mDhcpCheckBox.setChecked(true);
                mStaticCheckBox.setChecked(false);
            } else if (mode == IpAssignment.STATIC) {
                getEthInfoFromStaticIp();
                mDhcpCheckBox.setChecked(false);
                mStaticCheckBox.setChecked(true);
            }
        }
    }

    private void refreshUI() {
        mEthernetAddressEdit.setText(mEthIpAddress);
        mEthernetMaskEdit.setText(mEthNetmask);
        mEthernetGateWayEdit.setText(mEthGateway);
        mEthernetDnsEdit.setText(mEthdns1);
        mEthernetDns2Edit.setText(mEthdns2);
        updateCheckBox();
    }

    private void updatePreference(boolean isEnable) {
        mEthernetAddressEdit.setEnabled(isEnable);
        mEthernetMaskEdit.setEnabled(isEnable);
        mEthernetGateWayEdit.setEnabled(isEnable);
        mEthernetDnsEdit.setEnabled(isEnable);
        mEthernetDns2Edit.setEnabled(isEnable);
    }

    private void updateCheckBox() {
        String[] ifaces = mEthManager.getAvailableInterfaces();
        if (mEthManager != null) {
            if (ifaces.length > 0) {
                mInterfaceName = ifaces[0];
                boolean useDhcp = (mEthManager.getConfiguration(mInterfaceName).getIpAssignment() == IpAssignment.DHCP) ? true : false;
                if (useDhcp == true) {
                    updatePreference(false);
                } else {
                    updatePreference(true);
                }
            }
        }
    }


    public void getEthInfoFromDhcp() {
        String tempIpInfo;

        tempIpInfo = /*SystemProperties.get("dhcp."+ iface +".ipaddress");*/
                mEthManager.getIpAddress(mInterfaceName);
        Log.i(TAG, "getEthInfoFromDhcp: tempIpInfo " + tempIpInfo);
        if ((tempIpInfo != null) && (!tempIpInfo.equals(""))) {
            mEthIpAddress = tempIpInfo;
        } else {
            mEthIpAddress = mNullIpInfo;
        }
        Log.i(TAG, "getEthInfoFromDhcp: mEthIpAddress " + mEthIpAddress);
        tempIpInfo = /*SystemProperties.get("dhcp."+ iface +".mask");*/
                mEthManager.getNetmask(mInterfaceName);
        if ((tempIpInfo != null) && (!tempIpInfo.equals(""))) {
            mEthNetmask = tempIpInfo;
        } else {
            mEthNetmask = mNullIpInfo;
        }
        Log.i(TAG, "getEthInfoFromDhcp: mEthNetmask " + mEthNetmask);
        tempIpInfo = /*SystemProperties.get("dhcp."+ iface +".gateway");*/
                mEthManager.getGateway(mInterfaceName);
        if ((tempIpInfo != null) && (!tempIpInfo.equals(""))) {
            mEthGateway = tempIpInfo;
        } else {
            mEthGateway = mNullIpInfo;
        }
        Log.i(TAG, "getEthInfoFromDhcp: mEthGateway " + mEthGateway);
        tempIpInfo = /*SystemProperties.get("dhcp."+ iface +".dns1");*/
                mEthManager.getDns(mInterfaceName);
        if ((tempIpInfo != null) && (!tempIpInfo.equals(""))) {
            String data[] = tempIpInfo.split(",");
            mEthdns1 = data[0];
            if (data.length <= 1) {
                mEthdns2 = mNullIpInfo;
            } else {
                mEthdns2 = data[1];
            }
        } else {
            mEthdns1 = mNullIpInfo;
        }
    }


    public void getEthInfoFromStaticIp() {
        StaticIpConfiguration staticIpConfiguration = mEthManager.getConfiguration(mInterfaceName).getStaticIpConfiguration();

        if (staticIpConfiguration == null) {
            return;
        }
        LinkAddress ipAddress = staticIpConfiguration.getIpAddress();
        InetAddress gateway = staticIpConfiguration.getGateway();
        List<InetAddress> dnsServers = staticIpConfiguration.getDnsServers();

        if (ipAddress != null) {
            mEthIpAddress = ipAddress.getAddress().getHostAddress();
            mEthNetmask = interMask2String(ipAddress.getPrefixLength());
        }
        if (gateway != null) {
            mEthGateway = gateway.getHostAddress();
        }
        mEthdns1 = dnsServers.get(0).getHostAddress();

        if (dnsServers.size() > 1) { /* 只保留两个*/
            mEthdns2 = dnsServers.get(1).getHostAddress();
        }

    }


    //将子网掩码转换成ip子网掩码形式，比如输入32输出为255.255.255.255
    public String interMask2String(int prefixLength) {
        String netMask = null;
        int inetMask = prefixLength;

        int part = inetMask / 8;
        int remainder = inetMask % 8;
        int sum = 0;

        for (int i = 8; i > 8 - remainder; i--) {
            sum = sum + (int) Math.pow(2, i - 1);
        }

        if (part == 0) {
            netMask = sum + ".0.0.0";
        } else if (part == 1) {
            netMask = "255." + sum + ".0.0";
        } else if (part == 2) {
            netMask = "255.255." + sum + ".0";
        } else if (part == 3) {
            netMask = "255.255.255." + sum;
        } else if (part == 4) {
            netMask = "255.255.255.255";
        }

        return netMask;
    }


    private boolean setStaticIpConfiguration() {
        Log.i(TAG, "setStaticIpConfiguration: ===============");
        /*
         * get ip address, netmask,dns ,gw etc.
         */
        InetAddress inetAddr = getIPv4Address(this.mEthIpAddress);
        int prefixLength = maskStr2InetMask(this.mEthNetmask);
        InetAddress gatewayAddr = getIPv4Address(this.mEthGateway);
        InetAddress dnsAddr = getIPv4Address(this.mEthdns1);

        if (null == inetAddr || inetAddr.getAddress().toString().isEmpty()
                || prefixLength == 0
                || gatewayAddr.toString().isEmpty()
                || dnsAddr.toString().isEmpty()) {
            Log.i(TAG, "ip,mask or dnsAddr is wrong");
            return false;
        }

        String dnsStr2 = this.mEthdns2;
        ArrayList<InetAddress> dnsAddrs = new ArrayList<InetAddress>();
        dnsAddrs.add(dnsAddr);
        if (!dnsStr2.isEmpty()) {
            dnsAddrs.add(getIPv4Address(dnsStr2));
        }
        mStaticIpConfiguration = new StaticIpConfiguration.Builder()
                .setIpAddress(new LinkAddress(inetAddr, prefixLength))
                .setGateway(gatewayAddr)
                .setDnsServers(dnsAddrs)
                .build();

        mIpConfiguration = new IpConfiguration();
        mIpConfiguration.setIpAssignment(IpAssignment.STATIC);
        mIpConfiguration.setProxySettings(IpConfiguration.ProxySettings.NONE);
        mIpConfiguration.setStaticIpConfiguration(mStaticIpConfiguration);
        return true;
    }


    private Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) InetAddresses.parseNumericAddress(text);
        } catch (IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }


    /*
     * convert subMask string to prefix length
     */
    private int maskStr2InetMask(String maskStr) {
        StringBuffer sb;
        String str;
        int inetmask = 0;
        int count = 0;
        /*
         * check the subMask format
         */
        Pattern pattern = Pattern.compile("(^((\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])$)|^(\\d|[1-2]\\d|3[0-2])$");
        if (pattern.matcher(maskStr).matches() == false) {
            Log.e(TAG, "subMask is error");
            return 0;
        }

        String[] ipSegment = maskStr.split("\\.");
        for (int n = 0; n < ipSegment.length; n++) {
            sb = new StringBuffer(Integer.toBinaryString(Integer.parseInt(ipSegment[n])));
            str = sb.reverse().toString();
            count = 0;
            for (int i = 0; i < str.length(); i++) {
                i = str.indexOf("1", i);
                if (i == -1)
                    break;
                count++;
            }
            inetmask += count;
        }
        return inetmask;
    }


    /*
     * 返回 指定的 String 是否是 有效的 IP 地址.
     */
    private boolean isValidIpAddress(String value) {
        int start = 0;
        int end = value.indexOf('.');
        int numBlocks = 0;

        while (start < value.length()) {

            if (-1 == end) {
                end = value.length();
            }

            try {
                int block = Integer.parseInt(value.substring(start, end));
                if ((block > 255) || (block < 0)) {
                    Log.w("EthernetIP",
                            "isValidIpAddress() : invalid 'block', block = "
                                    + block);
                    return false;
                }
            } catch (NumberFormatException e) {
                Log.w("EthernetIP", "isValidIpAddress() : e = " + e);
                return false;
            }

            numBlocks++;

            start = end + 1;
            end = value.indexOf('.', start);
        }
        return numBlocks == 4;
    }

}
