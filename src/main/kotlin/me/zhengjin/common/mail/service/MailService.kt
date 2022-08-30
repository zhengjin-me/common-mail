package me.zhengjin.common.mail.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.UnsupportedEncodingException

/**
 * @version V1.0
 * title: 邮件发送服务
 * package
 * description:
 * @author fangzhengjin
 * cate 2018-7-25 15:45
 */
@Service
class MailService(
    private val mailSender: JavaMailSender,
) {

    @Value("\${spring.mail.username}")
    lateinit var from: String

    fun mailAddressCheck(mailAddress: String) {
        val email =
            Regex("^(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$")
        if (!email.matches(mailAddress)) throw IllegalArgumentException("无效的邮箱地址")
    }

    /**
     * 异步方法
     * @param to                    收件人
     * @param subject               邮件标题
     * @param template              邮件模板字符串,填充的变量填写在{{{}}}中
     * @param template              邮件模板填充内容
     * @param attachmentName        附件名称(可选)
     * @param attachmentByteArray   附件对象(可选,名称填写则必填)
     * @param prefix                变量左侧包裹标识,默认: {{{
     * @param suffix                变量右侧包裹标识,默认: }}}
     */
    @Async
    @Throws(MailException::class, UnsupportedEncodingException::class)
    fun sendTemplateMail(
        to: String,
        subject: String,
        template: String,
        templateParameter: Map<String, String>,
        attachmentName: String? = null,
        attachmentByteArray: ByteArray? = null,
        prefix: String = "{{{",
        suffix: String = "}}}",
    ) {
        var content = template
        templateParameter.forEach { (k, v) ->
            content = content.replace("${prefix}${k}$suffix", v)
        }
        if (!attachmentName.isNullOrBlank() && attachmentByteArray != null) {
            sendHtmlAndAttachmentMail(to, subject, content, attachmentName, attachmentByteArray)
        } else {
            sendMail(to, subject, content, true)
        }
    }

    /**
     * @param to                收件人
     * @param subject           邮件标题
     * @param content           邮件正文
     * @param isHtmlContent     邮件正文是否为Html
     */
    @Throws(MailException::class)
    fun sendMail(to: String, subject: String, content: String, isHtmlContent: Boolean = true) {
        val mailAddress = to.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // 邮件地址有效性校验
        mailAddress.forEach { mailAddressCheck(it) }
        val mail = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mail)
        helper.setTo(mailAddress) // 发送给谁
        helper.setSubject(subject) // 标题
        helper.setFrom(from) // 来自
        // 邮件内容，第二个参数指定发送的是HTML格式
        helper.setText(content, isHtmlContent)
        mailSender.send(mail)
    }

    /**
     * @param to                    收件人
     * @param subject               邮件标题
     * @param content               邮件正文(支持html)
     * @param attachmentName        附件名称
     * @param attachmentByteArray   附件对象
     */
    @Throws(MailException::class, UnsupportedEncodingException::class)
    fun sendHtmlAndAttachmentMail(
        to: String,
        subject: String,
        content: String,
        attachmentName: String,
        attachmentByteArray: ByteArray
    ) {
        val mailAddress = to.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // 邮件地址有效性校验
        mailAddress.forEach { mailAddressCheck(it) }
        val mail = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mail, true, "UTF-8")
        helper.setTo(mailAddress) // 发送给谁
        helper.setSubject(subject) // 标题
        helper.setFrom(from) // 来自
        // 邮件内容，第二个参数指定发送的是HTML格式
        helper.setText(content, true)
//        if (Objects.isNull(objectToByte)) {
//            throw ServiceException("附件对象不能为空")
//        }
        val iss = ByteArrayResource(attachmentByteArray)
        helper.addAttachment(attachmentName, iss)
        mailSender.send(mail)
    }
}
