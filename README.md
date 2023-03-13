# EthernetSetting

**以太网设置调用API**

#### 功能说明

通过EthernetManager 去设置以太网、获取以太网模式，数据等。Demo可参考ethernetcontroldemo.rar

```java
EthernetManager mEthManager = (EthernetManager) getActivity().getSystemService(Context.ETHERNET_SERVICE);//获取系统以太网管理类
```

#### 接口说明
##### 1、获取有效的以太网接口。
>返回值：以太网名称数组
```java

String[] ifaces = mEthManager.getAvailableInterfaces()

```
##### 2、根据以太网接口获取以太网模式
```java
IpAssignment mode = mEthManager.getConfiguration(mInterfaceName).getIpAssignment();
```

##### 3、获取静态IP时的IP信息
```java
StaticIpConfiguration staticIpConfiguration =mEthManager.getConfiguration(mInterfaceName).getStaticIpConfiguration();
```

##### 4、获取动态Ip时的IP信息
>参数说明：mInterfaceName 以太网名称
```java
String iPAddress=mEthManager.getIpAddress(mInterfaceName);//ip地址
String netmask=mEthManager.getNetmask(mInterfaceName);//子网掩码
String gateway=mEthManager.getGateway(mInterfaceName);//网关
String[]dns=mEthManager.getDns(mInterfaceName);//dns
```
##### 5、设置以太网模式
>参数说明：mInterfaceName 以太网名称 mIpConfiguration Ip配置信息
```java
//设置静态Ip
IpConfiguration mIpConfiguration = new IpConfiguration();
mIpConfiguration.setIpAssignment(IpAssignment.STATIC);
mIpConfiguration.setProxySettings(IpConfiguration.ProxySettings.NONE);
mIpConfiguration.setStaticIpConfiguration(mStaticIpConfiguration);
mEthManager.setConfiguration(mInterfaceName, mIpConfiguration);
//设置动态获取
IpConfiguration ipConfiguration = new IpConfiguration();
ipConfiguration.setIpAssignment(IpAssignment.DHCP);
ipConfiguration.setProxySettings(IpConfiguration.ProxySettings.NONE);
mEthManager.setConfiguration(mInterfaceName, ipConfiguration);

```
##### 6、打开/关闭以太网
>参数说明：enable true:使能 false:关闭

```java
mEthManager.setEnabled(enable);
```
##### 6、设置广播监听以太网状态
```java
private final static String ETHERNET_ACTION = "android.intent.action.ETHERNET_LINEKSTATE";
...
mIntentFilter = new IntentFilter();
mIntentFilter.addAction(ETHERNET_ACTION);
mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
...
String eth0state = intent.getStringExtra("eth0");
if (!TextUtils.isEmpty(eth0state)) {
    if ("up".equals(eth0state)) {
        Toast.makeText(mContext, "网线已接上!", Toast.LENGTH_SHORT).show();
    } else {
        Toast.makeText(mContext, "网线已断开!", Toast.LENGTH_SHORT).show();
    
    }
}
```
