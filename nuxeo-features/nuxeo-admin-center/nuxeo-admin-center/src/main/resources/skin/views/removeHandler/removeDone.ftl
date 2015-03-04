<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">

 <div class="successfulDownloadBox">
        <h1> Removal of ${pkgId?xml} completed </h1>

    <br/>

    <a href="${Root.path}/packages/${source?xml}" class="installButton"> Finish </a>
 </div>

</@block>
</@extends>