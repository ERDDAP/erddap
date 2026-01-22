@param String title
@param String cssHref
@param String preTableHtml
@param String postTableHtml
@param boolean isMBLimited
@param String htmlTableMaxMessage
@param boolean xhtmlMode
@param boolean writeHeader
@param String[] columnNames
@param String[] units
@param boolean[] rightAlignCol
@param java.util.List<String[]> rows
@param String headHtml
@param String bodyHtml
@param String endBodyHtml
@if(xhtmlMode)
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="https://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
  <title>${title}</title>
  <link rel="stylesheet" type="text/css" href="${cssHref}erddap2.css" />
</head>
<body>
$unsafe{bodyHtml}
@if(preTableHtml != null && preTableHtml.length() > 0)
  $unsafe{preTableHtml}
@endif
&nbsp;
@template.table.htmlTable_rows(
  writeHeader = writeHeader,
  columnNames = columnNames,
  rightAlignCol = rightAlignCol,
  units = units,
  rows = rows,
  xhtmlMode = xhtmlMode,
  isMBLimited = isMBLimited,
  htmlTableMaxMessage = htmlTableMaxMessage
)
@if(postTableHtml != null && postTableHtml.length() > 0)
  $unsafe{postTableHtml}
@endif
$unsafe{endBodyHtml}
</body>
</html>
@else
$unsafe{headHtml}
$unsafe{bodyHtml}
@if(preTableHtml != null && preTableHtml.length() > 0)
  $unsafe{preTableHtml}
@endif
&nbsp;
@template.table.htmlTable_rows(
  writeHeader = writeHeader,
  columnNames = columnNames,
  rightAlignCol = rightAlignCol,
  units = units,
  rows = rows,
  xhtmlMode = xhtmlMode,
  isMBLimited = isMBLimited,
  htmlTableMaxMessage = htmlTableMaxMessage
)
@if(postTableHtml != null && postTableHtml.length() > 0)
  $unsafe{postTableHtml}
@endif
$unsafe{endBodyHtml}
@endif