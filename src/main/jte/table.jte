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
                <th>${tableOptions.getColumnHeader(col)}</th>
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
                                !{var separatedText = tableOptions.getExtractUrls(s);}
                                @for(String text : separatedText)
                                    @if(tableOptions.getContainsUrl(text))
                                        <a href="${tableOptions.encodeAddHttps(text)}">
                                            ${tableOptions.getEncodeAsHtml(text)}
                                        </a>
                                    @else
                                        <p>${tableOptions.getEncodeAsHtml(text)}</p>
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