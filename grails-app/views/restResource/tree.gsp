<%@ page import="au.org.biodiversity.nsl.*; au.org.biodiversity.nsl.tree.*" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Tree ${tree?.label ?: tree?.id}</title>
</head>

<body>
<div class="rest-resource-content tree-gsp">
    <h3>Tree ${tree?.label ?: tree?.id}</h3>

    <dl class="dl-horizontal">
        <dt>Id</dt><dd>${tree.id}</dd>
        <g:if test="${tree.label}"><dt>Label</dt><dd>${tree.label}</dd></g:if>
        <g:if test="${tree.title}"><dt>Title</dt><dd>${tree.title}</dd></g:if>
        <g:if test="${tree.description}"><dt>Description</dt><dd>${tree.description}</dd></g:if>
        <dt>Root node</dt><dd><td><g:render template="node" model="${[node: tree.node]}"/></td></dd>
    </dl>

    <% /* TODO: include some stats here */ %>

    <g:render template="links"/>

</div>
</body>
</html>

