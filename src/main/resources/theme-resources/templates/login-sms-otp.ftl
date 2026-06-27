<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        ${msg("bulkgateFormTitle")}
    <#elseif section = "form">
        <p class="instruction">${msg("bulkgateInstruction", (ttlMinutes!5)?string)}</p>

        <form id="kc-sms-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="code" class="${properties.kcLabelClass!}">${msg("bulkgateCodeLabel")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="code" name="code" autocomplete="one-time-code"
                           inputmode="numeric" pattern="[0-9]*"
                           class="${properties.kcInputClass!}" autofocus aria-required="true"/>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("bulkgateSubmit")}"/>
                </div>
            </div>
        </form>

        <form id="kc-sms-otp-resend-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="resend" value="true"/>
            <div class="${properties.kcFormGroupClass!}">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                       type="submit" value="${msg("bulkgateResend")}"/>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
