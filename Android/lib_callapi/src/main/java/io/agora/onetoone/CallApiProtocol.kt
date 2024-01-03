package io.agora.onetoone

import android.view.TextureView
import android.view.ViewGroup
import io.agora.rtc2.RtcEngineEx
import io.agora.rtm.RtmClient

open class CallConfig(
    //声网App Id
    var appId: String = "",
    //用户id，通过该用户id来发送信令消息
    var userId: Int = 0,
    //rtc engine实例
    var rtcEngine: RtcEngineEx? = null,
    //[可选]rtm client实例，如果设置则需要负责rtmClient的login和logout，需要使用appId和userId创建
    var rtmClient: RtmClient? = null,
){}

open class PrepareConfig(
    var roomId: String = "",                //频道名(主叫需要设置为1v1的频道，被叫可设置为自己的广播频道)
    var rtcToken: String = "",              //rtc token，需要使用万能token，token创建的时候channel name为空字符串
    var rtmToken: String = "",              //rtm token
    var localView: ViewGroup? = null,       //显示本地流的画布
    var remoteView: ViewGroup? = null,      //显示远端流的画布
    var autoAccept: Boolean = true,         //被叫收到呼叫后是否自动接受，true: CallApi内部会自动调用accept，false: 外部收到calling状态时需要手动accept/reject
    var autoJoinRTC: Boolean = false,       //是否自动登录RTC
) {}

enum class CallReason(val value: Int) {
    None(0),
    JoinRTCFailed(1),           // 加入RTC失败
    RtmSetupFailed(2),          // 设置RTM失败
    RtmSetupSuccessed(3),       // 设置RTM成功
    MessageFailed(4),           // 消息发送失败
    LocalRejected(5),           // 本地用户拒绝
    RemoteRejected(6),          // 远端用户拒绝
    RemoteAccepted(7),          // 远端用户接受
    LocalAccepted(8),           // 本地用户接受
    LocalHangup(9),             // 本地用户挂断
    RemoteHangup(10),           // 远端用户挂断
    LocalCancel(11),            // 本地用户取消呼叫
    RemoteCancel(12),           // 远端用户取消呼叫
    RecvRemoteFirstFrame(13),   // 收到远端首帧
    CallingTimeout (14),        // 呼叫超时
    CancelByCallerRecall(15),   // 同样的主叫呼叫不同频道导致取消
    RtmLost(16)                 //rtm超时断连
}

enum class CallEvent(val value: Int) {
    None(0),
    Deinitialize(1),                // 调用了deinitialize
    MissingReceipts(2),             // 没有收到消息回执
    CallingTimeout(3),              // 呼叫超时
    JoinRTCFailed(4),               // 加入RTC失败
    JoinRTCSuccessed(5),            // 加入RTC成功
    RtmSetupFailed(6),              // 设置RTM失败
    RtmSetupSuccessed(7),           // 设置RTM成功
    MessageFailed(8),               // 消息发送失败
    StateMismatch(9),               // 状态流转异常
    PreparedRoomIdChanged(10),      //prepared了另一个roomId导致
    RemoteUserRecvCall(99),         //主叫呼叫成功
    LocalRejected(100),             // 本地用户拒绝
    RemoteRejected(101),            // 远端用户拒绝
    OnCalling(102),                 // 变成呼叫中
    RemoteAccepted(103),            // 远端用户接收
    LocalAccepted(104),             // 本地用户接收
    LocalHangup(105),               // 本地用户挂断
    RemoteHangup(106),              // 远端用户挂断
    RemoteJoin(107),                // 远端用户加入RTC频道
    RemoteLeave(108),               // 远端用户离开RTC频道
    LocalCancel(109),               // 本地用户取消呼叫
    RemoteCancel(110),              // 远端用户取消呼叫
    LocalJoin(111),                 // 本地用户加入RTC频道
    LocalLeave(112),                // 本地用户离开RTC频道
    RecvRemoteFirstFrame(113),      // 收到远端首帧
    CancelByCallerRecall(114),      // 同样的主叫呼叫不同频道导致取消
    RtmLost(115),                   //rtm超时断连
    RtcOccurError(116)              //rtc出现错误
}

/**
 * 呼叫状态类型
 */
enum class CallStateType(val value: Int) {
    Idle(0),            // 空闲
    Prepared(1),        // 创建1v1环境完成
    Calling(2),         // 呼叫中
    Connecting(3),      // 连接中
    Connected(4),       // 通话中
    Failed(10);         // 出现错误

    companion object {
        fun fromValue(value: Int): CallStateType {
            return values().find { it.value == value } ?: Idle
        }
    }
}
enum class CallLogLevel(val value: Int) {
    Normal(0),
    Warning(1),
    Error(2),
}
interface ICallApiListener {
    /**
     * 状态响应回调
     * @param state 状态类型
     * @param stateReason 状态原因
     * @param eventReason 事件类型描述
     * @param eventInfo 扩展信息，不同事件类型参数不同，其中key为“publisher”为状态变更者id，空则表示是自己的状态变更
     */
    fun onCallStateChanged(state: CallStateType,
                           stateReason: CallReason,
                           eventReason: String,
                           eventInfo: Map<String, Any>)

    /**
     * 内部详细事件变更回调
     * @param event: 事件
     */
    fun onCallEventChanged(event: CallEvent) {}

    /** token快要过期了(需要外部获取新token调用renewToken更新)
     */
    fun tokenPrivilegeWillExpire() {}

    /** 日志回调
     *  @param message: 日志信息
     *  @param logLevel: 日志优先级: 0: 普通日志，1: 警告日志, 2: 错误日志
     */
    fun callDebugInfo(message: String, logLevel: CallLogLevel) {}
}

data class AGError(
    val msg: String,
    val code: Int
)

interface ICallApi {

    /** 初始化配置
     * @param config
     */
    fun initialize(config: CallConfig)

    /** 释放缓存 */
    fun deinitialize(completion: (() -> Unit))

    /** 更新自己的rtc/rtm的token
     */
    fun renewToken(rtcToken: String, rtmToken: String)

    /** 连接(对RTM进行login和subscribe)， 观众调用
     *
     * @param prepareConfig
     * @param completion
     */
    fun prepareForCall(prepareConfig: PrepareConfig, completion: ((AGError?) -> Unit)?)

    /** 监听远端处理的回调
     *
     * @param listener
     */
    fun addListener(listener: ICallApiListener)

    /** 取消监听远端回调
     * @param listener
     */
    fun removeListener(listener: ICallApiListener)

    /** 发起通话，加 RTC 频道并且发流，并且发 rtm 频道消息 申请链接，调用后被叫会收到onCall
     *
     * @param remoteUserId 呼叫的用户id
     * @param completion
     */
    fun call(remoteUserId: Int, completion: ((AGError?) -> Unit)?)

    /** 取消正在发起的通话，未接通的时候可用，调用后被叫会收到onCancel
     *
     * @param completion
     */
    fun cancelCall(completion: ((AGError?) -> Unit)?)

    /** 接受通话，调用后主叫会收到onAccept
     *
     * @param remoteUserId: 呼叫的用户id
     * @param completion: <#completion description#>
     */
    fun accept(remoteUserId: Int, completion: ((AGError?) -> Unit)?)

    /** 被叫拒绝通话，调用后主叫会收到onReject
     *
     * @param remoteUserId 呼叫的用户id
     * @param reason 拒绝原因
     * @param completion
     */
    fun reject(remoteUserId: Int, reason: String?, completion: ((AGError?) -> Unit)?)

    /** 结束通话，调用后被叫会收到onHangup
     * @param remoteUserId 用户id
     * @param completion
     */
    fun hangup(remoteUserId: Int, completion: ((AGError?) -> Unit)?)

    /** 获取callId，callId为通话过程中消息的标识，通过argus可以查询到从呼叫到通话的耗时和状态变迁的时间戳
     * @return callId，非呼叫到通话之外的消息为空
     */
    fun getCallId(): String
}