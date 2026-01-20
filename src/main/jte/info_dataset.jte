@import gov.noaa.pfel.erddap.jte.TableOptions
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.jte.YouAreHere
@import jakarta.servlet.http.HttpServletRequest
@import gov.noaa.pfel.erddap.util.EDMessages.Message

@param TableOptions dsTableOptions
@param TableOptions infoTableOptions
@param String tErddapUrl
@param int language
@param YouAreHere youAreHere
@param String loggedInAs
@param HttpServletRequest request
@param String plainFileTypesString
@param String jsonldScript

@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=null,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
<div class="wide_max_width">

    @if(dsTableOptions != null)
        @template.components.table(tableOptions=dsTableOptions)
    @endif

    <h2>${TemplateHelper.getMessage(Message.INFO_TABLE_TITLE_HTML, language)}</h2>

    <table class="erd commonBGColor">
        <tr>
            @for(int col = 0; col < infoTableOptions.getNColumns(); col++)
                <th>${infoTableOptions.getColumnHeader(col)}</th>
            @endfor
        </tr>

        @for(int row = 0; row < infoTableOptions.getNRows(); row++)
            !{var firstCol = infoTableOptions.getStringData(0, row);}
            <tr class="${ (firstCol.equals("variable") || firstCol.equals("dimension")) ? "highlightBGColor" : ""}">
            @for(int col = 0; col < infoTableOptions.getNColumns(); col++)
                !{var cell = infoTableOptions.getStringData(col, row);}
                <td>
                    @if(cell == null || cell.length() == 0)
                        &nbsp;
                    @else
                        $unsafe{cell}
                    @endif
                </td>
            @endfor
            </tr>
        @endfor
    </table>

    <p>
        ${TemplateHelper.getMessage(Message.RESTFUL_INFORMATION_FORMATS, language)}
        (${plainFileTypesString})
        <a rel="help" href="${tErddapUrl}/rest.html">
            ${TemplateHelper.getMessage(Message.RESTFUL_VIA_SERVICE, language)}
        </a>.
    </p>

    @if(jsonldScript != null && jsonldScript.length() > 0)
        $unsafe{jsonldScript}
    @endif

</div>
`)
