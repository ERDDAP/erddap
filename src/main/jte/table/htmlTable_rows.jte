@param boolean writeHeader
@param String[] columnNames
@param String[] units
@param boolean[] rightAlignCol
@param java.util.List<String[]> rows
@param boolean xhtmlMode
@param boolean isMBLimited
@param String htmlTableMaxMessage
<table class="erd commonBGColor nowrap">
@if(writeHeader)
  <thead>
    <tr>
    @for(int i = 0; i < columnNames.length; i++)
      <th>${columnNames[i]}</th>
    @endfor
    </tr>
    @if(units != null)
      <tr>
      @for(int i = 0; i < units.length; i++)
        <th>${units[i]}</th>
      @endfor
      </tr>
    @endif
  </thead>
@endif
<tbody>
@for(String[] row : rows)
  <tr>
  @for(int i = 0; i < row.length; i++)
    @if(rightAlignCol[i])
    <td class="R">$unsafe{row[i]}</td>
    @else
    <td>$unsafe{row[i]}</td>
    @endif
  @endfor
  </tr>
@endfor
</tbody>
</table>
@if(isMBLimited)
  <span class="warningColor">$unsafe{htmlTableMaxMessage}</span>
  <br />
@endif