@import gov.noaa.pfel.erddap.jte.TableOptions
@import gov.noaa.pfel.coastwatch.pointdata.Table
@param TableOptions tableOptions

<table class="erd${tableOptions.getWrap()}${tableOptions.getOtherClasses()}" style="background-color:${tableOptions.getBgColor()};">

    
    <!-- Table Headings -->
    <tr>
        @for(int col = 0; col < tableOptions.getNColumns(); col++)
            <th>${tableOptions.insideForLoophead(col)}</th>
        @endfor
    </tr>

    <!-- Units Row (if applicable) -->
    @if(tableOptions.getWriteUnits())
        <tr>
            !{tableOptions.setSomethingWritten(false);}
            @for(int col = 0; col < tableOptions.getNColumns(); col++)
                <th>${tableOptions.insideForLoopUnits(col)}</th>
            @endfor
        </tr>
    @endif

    <!-- Data Rows -->
    @for(int row = 0; row < tableOptions.getNRows(); row++)
        <tr>
            !{tableOptions.setSomethingWritten(false);}
            @for(int col = 0; col < tableOptions.getNColumns(); col++)
                <td>
                    @if(col == tableOptions.getTimeColumn())
                        ${tableOptions.getDoubleDataConverted(col, row)}
                    @else
                        !{var s = tableOptions.getStringData(col, row);}
                        @if(tableOptions.checkLengthFile(col))
                            <a href="${tableOptions.getUrl(col, s)}">
                                ${tableOptions.getEncodeashtml(s)}
                            </a>

                        @elseif(tableOptions.checkcontainsurl(s) || tableOptions.checkmouseover(s))
                            !{var separatedText = tableOptions.getExtractUrls(s);}
                            @for(String text : separatedText)
                                @if(tableOptions.getContainsUrl(text))
                                    <a href="${tableOptions.encodeaddhttps(text)}">
                                        ${tableOptions.getEncodeashtml(text)}
                                    </a>
                                @else
                                    <p>${tableOptions.getEncodeashtml(text)}</p>
                                @endif
                            @endfor

                        @elseif(tableOptions.checkDecodeEntities(s))
                            <a href="${tableOptions.getEncodeashtmlattribute(s)}">
                                ${tableOptions.getEncodeashtml(s)}
                            </a>

                        @elseif(tableOptions.checkIsEmail(s))
                            ${tableOptions.getEmailData(s)}

                        @elseif(tableOptions.checkEncodingAsHtml())
                            ${tableOptions.getEncodeashtml(s)}
                        @else
                            $unsafe{s}
                        @endif
                    @endif
                </td>
            @endfor
        </tr>
    @endfor

</table>
