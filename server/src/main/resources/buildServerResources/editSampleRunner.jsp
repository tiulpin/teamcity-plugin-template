<%@ include file="/include-internal.jsp" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Sample Runner">

    <tr>
        <th><label for="sample.message">Message: <l:star/></label></th>
        <td>
            <props:textProperty name="sample.message" className="longField" maxlength="256"/>
            <span class="smallNote">Logged once per iteration. Include 'FAIL' to make the build fail.</span>
            <span class="error" id="error_sample.message"></span>
        </td>
    </tr>

    <tr>
        <th><label for="sample.repeat">Repeat: <l:star/></label></th>
        <td>
            <props:textProperty name="sample.repeat" className="mediumField" maxlength="3"/>
            <span class="smallNote">How many times to log the message (1-100).</span>
            <span class="error" id="error_sample.repeat"></span>
        </td>
    </tr>

</l:settingsGroup>
