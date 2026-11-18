@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.jte.TabledapVariable
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import com.cohort.util.XML
@import jakarta.servlet.http.HttpServletRequest
@import java.util.List

@param YouAreHere youAreHere
@param int language
@param String tErddapUrl
@param String loggedInAs = ""
@param String endOfRequest = ""
@param HttpServletRequest request = null

@param String datasetInfoHtml = ""
@param String formName = "form1"
@param List<TabledapVariable> variables = List.of()
@param List<String> operators = List.of("=", "!=", "<", "<=", ">", ">=", "=~")
@param boolean hasDistinctOptions = false
@param String checkAllJs = ""
@param String uncheckAllJs = ""
@param String addVariablesWhereHtml = ""
@param String functionHtml = ""
@param List<String> fileTypeDescriptions = List.of()
@param String defaultFileTypeOption = ".htmlTable"
@param String javaScript = ""
@param String sliderScriptHtml = ""
@param String dasContent = ""
@param String dapBase = ""
@param String exampleHref = ""
@param String exampleText = ""
@param String queryExplanationHtml = ""
@param String longDapDescription = ""

@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
!{
    int eqIndex = operators.indexOf("=");
}
<script src="${tErddapUrl}/images/wz_dragdrop.js"></script>
<div class="standard_width">
    $unsafe{datasetInfoHtml}

    &nbsp;
    @template.components.noscript()

    <form action="" method="get" name="${formName}">
        <script>
        function percentEncode(s) {
          var s2="";
          for (var i = 0; i < s.length; i++) {
            var ch=s.charAt(i);
            if (ch == "\xA0") s2+="%20";
            else s2+=encodeURIComponent(ch);
          }
          return s2;
        }
        </script>

        <table class="compact nowrap">
            <tr>
                <th class="L">
                    ${TemplateHelper.getMessage(Message.EDD_TABLE_VARIABLE, language)}
                    @template.components.tooltip_image(
                        request=request,
                        language=language,
                        loggedInAs=loggedInAs,
                        html="<div class=\"narrow_max_width\">" + TemplateHelper.getMessage(Message.EDD_TABLE_TABULAR_DATASET_TOOLTIP, language) + "</div>"
                    )
                    <input type="button" name="CheckAll"
                           value="${TemplateHelper.getMessage(Message.EDD_TABLE_CHECK_ALL, language)}"
                           onclick="${checkAllJs}"
                           onmouseover="Tip('${TemplateHelper.escapeTooltipForTip(TemplateHelper.getMessage(Message.EDD_TABLE_CHECK_ALL_TOOLTIP, language))}')"
                           onmouseout="UnTip()">
                    <input type="button" name="UncheckAll"
                           value="${TemplateHelper.getMessage(Message.EDD_TABLE_UNCHECK_ALL, language)}"
                           onclick="${uncheckAllJs}"
                           onmouseover="Tip('${TemplateHelper.escapeTooltipForTip(TemplateHelper.getMessage(Message.EDD_TABLE_UNCHECK_ALL_TOOLTIP, language))}')"
                           onmouseout="UnTip()">
                    &nbsp;
                </th>
                <th colspan="2">
                    $unsafe{TemplateHelper.getMessage(Message.EDD_TABLE_OPT_CONSTRAINT1_HTML, language)}
                    @template.components.tooltip_image(
                        request=request,
                        language=language,
                        loggedInAs=loggedInAs,
                        html="<div class=\"narrow_max_width\">" + TemplateHelper.getMessage(Message.EDD_TABLE_CONSTRAINT_TOOLTIP, language) + "</div>"
                    )
                </th>
                <th colspan="2">
                    $unsafe{TemplateHelper.getMessage(Message.EDD_TABLE_OPT_CONSTRAINT2_HTML, language)}
                    @template.components.tooltip_image(
                        request=request,
                        language=language,
                        loggedInAs=loggedInAs,
                        html="<div class=\"narrow_max_width\">" + TemplateHelper.getMessage(Message.EDD_TABLE_CONSTRAINT_TOOLTIP, language) + "</div>"
                    )
                </th>
                <th>
                    &nbsp;&nbsp;&nbsp;${TemplateHelper.getMessage(Message.EDD_MINIMUM, language)}
                    @template.components.tooltip_image(
                        request=request,
                        language=language,
                        loggedInAs=loggedInAs,
                        html=TemplateHelper.getMessage(Message.EDD_TABLE_MINIMUM_TOOLTIP, language)
                    )
                    @if(hasDistinctOptions)
                        <br>&nbsp;&nbsp;&nbsp;${TemplateHelper.getMessage(Message.OR_A_LIST_OF_VALUES, language)}
                        @template.components.tooltip_image(
                            request=request,
                            language=language,
                            loggedInAs=loggedInAs,
                            html="<div class=\"narrow_max_width\">" + TemplateHelper.getMessage(Message.DISTINCT_VALUES_TOOLTIP, language) + "</div>"
                        )
                    @else
                        <br>&nbsp;
                    @endif
                </th>
                <th>
                    &nbsp;&nbsp;&nbsp;${TemplateHelper.getMessage(Message.EDD_MAXIMUM, language)}
                    @template.components.tooltip_image(
                        request=request,
                        language=language,
                        loggedInAs=loggedInAs,
                        html=TemplateHelper.getMessage(Message.EDD_TABLE_MAXIMUM_TOOLTIP, language)
                    )
                    <br>&nbsp;
                </th>
            </tr>
            @for(TabledapVariable v : variables)
                <tr>
                    <td>
                        <input type="checkbox" name="varch${v.getIndex()}" id="varch${v.getIndex()}" value="${v.getName()}"
                               checked="${v.isChecked()}"
                               onmouseover="Tip('${TemplateHelper.escapeTooltipForTip(TemplateHelper.getMessage(Message.EDD_TABLE_CHECK_THE_VARIABLES, language))}')"
                               onmouseout="UnTip()">
                        <label for="varch${v.getIndex()}">$unsafe{v.getLabelHtml()}</label>
                        &nbsp;
                    </td>
                    <td>
                        <select name="op${v.getIndex()}_0" size="1"
                                onmouseover="Tip('${TemplateHelper.escapeTooltipForTip(TemplateHelper.getMessage(Message.EDD_TABLE_SELECT_AN_OPERATOR, language))}')"
                                onmouseout="UnTip()">
                            @for(String op : operators)
                                <option value="${op}" selected="${op.equals(v.getOp0())}">${op}</option>
                            @endfor
                        </select>
                    </td>
                    <td>
                        <input type="text" name="val${v.getIndex()}_0" size="19" maxlength="255" value="${v.getVal0()}"
                               onmouseover="Tip('${TemplateHelper.escapeTooltipForTip(v.getValTooltip())}')"
                               onmouseout="UnTip()">
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;<select name="op${v.getIndex()}_1" size="1"
                                onmouseover="Tip('${TemplateHelper.escapeTooltipForTip(TemplateHelper.getMessage(Message.EDD_TABLE_SELECT_AN_OPERATOR, language))}')"
                                onmouseout="UnTip()">
                            @for(String op : operators)
                                <option value="${op}" selected="${op.equals(v.getOp1())}">${op}</option>
                            @endfor
                        </select>
                    </td>
                    <td>
                        <input type="text" name="val${v.getIndex()}_1" size="19" maxlength="255" value="${v.getVal1()}"
                               onmouseover="Tip('${TemplateHelper.escapeTooltipForTip(v.getValTooltip())}')"
                               onmouseout="UnTip()">
                    </td>
                    @if(v.hasDistinctOptions())
                        <td colspan="2">
                            <table class="compact nowrap">
                                <tr>
                                    <td>&nbsp;&nbsp;<select name="dis${v.getIndex()}" size="1"
                                            onchange=" document.${formName}.op${v.getIndex()}_0.selectedIndex=${eqIndex}; document.${formName}.val${v.getIndex()}_0.value=this.value;">
                                            @for(int optIdx = 0; optIdx < v.getDistinctOptions().length; optIdx++)
                                                <option value="${v.getDistinctOptions()[optIdx]}" selected="${optIdx == v.getDistinctSelected()}">${v.getDistinctOptions()[optIdx]}</option>
                                            @endfor
                                        </select>
                                    </td>
                                    <td class="B">
                                        <img class="B" src="${tErddapUrl}/images/minus.gif"
                                             onmouseover="Tip('${TemplateHelper.escapeTooltipForTip(TemplateHelper.getMessage(Message.SELECT_PREVIOUS, language))}')"
                                             onmouseout="UnTip()"
                                             alt="-"
                                             onmouseup="var dis=document.${formName}.dis${v.getIndex()}; if (dis.selectedIndex>0) { dis.selectedIndex--; document.${formName}.op${v.getIndex()}_0.selectedIndex=${eqIndex}; document.${formName}.val${v.getIndex()}_0.value=dis.options[dis.selectedIndex].text; }">
                                    </td>
                                    <td class="B">
                                        <img class="B" src="${tErddapUrl}/images/plus.gif"
                                             onmouseover="Tip('${TemplateHelper.escapeTooltipForTip(TemplateHelper.getMessage(Message.SELECT_NEXT, language))}')"
                                             onmouseout="UnTip()"
                                             alt="+"
                                             onmouseup="var dis=document.${formName}.dis${v.getIndex()}; if (dis.selectedIndex<${v.getDistinctOptions().length - 1}) { dis.selectedIndex++; document.${formName}.op${v.getIndex()}_0.selectedIndex=${eqIndex}; document.${formName}.val${v.getIndex()}_0.value=dis.options[dis.selectedIndex].text; }">
                                    </td>
                                    <td>&nbsp;
                                        @template.components.tooltip_image(
                                            request=request,
                                            language=language,
                                            loggedInAs=loggedInAs,
                                            html="<div class=\"narrow_max_width\">" + TemplateHelper.getMessage(Message.EDD_TABLE_SELECT_CONSTRAINT_TOOLTIP, language) + "</div>"
                                        )
                                    </td>
                                </tr>
                            </table>
                        </td>
                    @else
                        <td>&nbsp;&nbsp;&nbsp;${v.getMinStr()}</td>
                        <td>&nbsp;&nbsp;&nbsp;${v.getMaxStr()}</td>
                    @endif
                </tr>
                @if(v.hasSlider())
                    <tr>
                        <td colspan="5">
                            $unsafe{v.getSliderHtml()}
                        </td>
                        <td colspan="2"></td>
                    </tr>
                @endif
            @endfor
        </table>

        @if(addVariablesWhereHtml != null && !addVariablesWhereHtml.isEmpty())
            $unsafe{addVariablesWhereHtml}
        @endif

        @if(functionHtml != null && !functionHtml.isEmpty())
            $unsafe{functionHtml}
        @endif

        @template.components.file_type_selector(
            tErddapUrl=tErddapUrl,
            protocol="tabledap",
            language=language,
            fileTypeDescriptions=fileTypeDescriptions,
            defaultFileTypeOption=defaultFileTypeOption
        )

        @template.components.form_actions(
            protocol="tabledap",
            tErddapUrl=tErddapUrl,
            language=language,
            loggedInAs=loggedInAs,
            request=request,
            javaScript=javaScript
        )

    </form>
    <br>&nbsp;

    @if(sliderScriptHtml != null && !sliderScriptHtml.isEmpty())
        $unsafe{sliderScriptHtml}
    @endif

    @template.components.das_section(
        language=language,
        dasContent=dasContent
    )

    @template.components.dap_instructions(
        protocol="tabledap",
        language=language,
        tErddapUrl=tErddapUrl,
        dapBase=dapBase,
        exampleHref=exampleHref,
        exampleText=exampleText,
        queryExplanationHtml=queryExplanationHtml,
        longDapDescription=longDapDescription
    )
</div>
`)


