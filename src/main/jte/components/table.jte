@import com.cohort.util.LinkHelper
@import com.cohort.util.XML
@import gov.noaa.pfel.erddap.jte.TableOptions
@import gov.noaa.pfel.coastwatch.pointdata.Table
@param TableOptions tableOptions
@if(tableOptions.getNRows()==0)
    <p>${tableOptions.getNoData()} (nRows =0)</p>
@else
<div class ="standard_width">
    <table class="erd${tableOptions.getWrap()}${tableOptions.getOtherClasses()}" style="background-color:${tableOptions.getBgColor()};">
        <!-- Table Headings -->
        <tr>
            @for(int col = 0; col < tableOptions.getNColumns(); col++)
                <%-- use unsafe here because certain tables like Erddap.makeHtmlDatasetTable include html in headers --%>
                <th>$unsafe{tableOptions.getColumnHeader(col)}</th>
            @endfor
        </tr>
        <!-- Units Row (if applicable) -->
        @if(tableOptions.getWriteUnits())
            <tr>
                @for(int col = 0; col < tableOptions.getNColumns(); col++)
                    <th>${tableOptions.getColumnUnits(col)}</th>
                @endfor
            </tr>
        @endif
        <!-- Data Rows -->
        @for(int row = 0; row < tableOptions.getNRows(); row++)
            <tr>
                @for(int col = 0; col < tableOptions.getNColumns(); col++)
                    <td>
                        @if(col == tableOptions.getTimeColumn())
                            ${tableOptions.getTimeString(col, row)}
                        @else
                            !{var s = tableOptions.getStringData(col, row);}
                            @if(tableOptions.checkLengthFile(col))
                                <a href="${tableOptions.getUrl(col, s)}">
                                    ${tableOptions.getEncodeAsHtml(s)}
                                </a>

                            @elseif(tableOptions.checkContainsUrl(s) || tableOptions.checkMouseOver(s))
                                @for(var segment : LinkHelper.segment(s))
                                    @if(segment.isUrl())
                                        <a href="${XML.encodeAsHTMLAttribute(segment.getHref())}">${tableOptions.getEncodeAsHtml(segment.getText())}</a>
                                    @else
                                        ${tableOptions.getEncodeAsHtml(segment.getText())}
                                    @endif
                                @endfor

                            @elseif(tableOptions.checkDecodeEntities(s))
                                <a href="${tableOptions.getEncodeAsHtmlAttribute(s)}">
                                    ${tableOptions.getEncodeAsHtml(s)}
                                </a>

                            @elseif(tableOptions.checkIsEmail(s))
                                ${tableOptions.getEmailData(s)}

                            @elseif(tableOptions.checkEncodingAsHtml())
                                ${tableOptions.getEncodeAsHtml(s)}
                            @else
                                $unsafe{s}
                            @endif
                        @endif
                    </td>
                @endfor
            </tr>
        @endfor
    </table>
</div>
@endif