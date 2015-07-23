<%--
  User: pmcneil
  Date: 15/09/14
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta name="layout" content="main">
  <title>APC Format - ${name.simpleName}</title>
</head>

<body>
<g:render template="/search/searchTabs"/>

<div class="panel  ${(params.product == 'apc' ? 'panel-success' : 'panel-info')} ">
  <div class="panel-heading">
    <strong>Showing ${name.simpleName}</strong>

    <div class="btn-group">
      <button id="fontToggle" class="btn btn-default" title="change font"><i class="fa fa-font"></i></button>
    </div>
  </div>

  <div class="panel-body">
    <g:render template="name" model="[name: name, apc: apc]"/>
  </div>
</div>
</body>
</html>