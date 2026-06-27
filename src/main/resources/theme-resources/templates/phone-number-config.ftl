<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        ${msg("bulkgatePhoneFormTitle")}
    <#elseif section = "form">
        <p class="instruction">${msg("bulkgatePhoneInstruction")}</p>

        <form id="kc-bulkgate-phone-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="mobileNumber" class="${properties.kcLabelClass!}">${msg("bulkgatePhoneLabel")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="tel" id="mobileNumber" name="mobileNumber" value="${(mobileNumber!'')}"
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
    </#if>
</@layout.registrationLayout>
