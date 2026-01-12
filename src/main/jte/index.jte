@import gov.noaa.pfel.erddap.jte.Index
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message

@param Index data

@template.layout.main(
    tErddapUrl=data.erddapUrl,
    language=data.language,
    youAreHere=data.youAreHere,
    endOfRequest=data.endOfRequest,
    loggedInAs=data.loggedInAs,
    request=data.request,
    content=@`
<div class="wide_max_width">
    <table style="vertical-align:top; width:100%; border:0; border-spacing:0px;">
        <tr>
            @if(data.shortDescriptionHtml != null)
                <td style="width:60%;" class="T">
                    $unsafe{data.shortDescriptionHtml}
                </td>
            @endif
            <td style="width:1%;">&nbsp;&nbsp;&nbsp;</td>
            <td style="width:1%;" class="verticalLine">&nbsp;&nbsp;&nbsp;</td>
            <td style="width:38%;" class="T">
                
                <h2>$unsafe{TemplateHelper.getMessage(Message.GET_STARTED_HTML, data.language)}</h2>
                <ul>
                    
                    <%-- Search Form --%>
                    <li>
                        @template.search_form(
                            language = data.language,
                            request = data.request,
                            loggedInAs = data.loggedInAs,
                            pretext = data.searchPretext,
                            posttext = data.searchPosttext,
                            searchFor = data.searchFor,
                            itemsPerPage = data.itemsPerPage,
                            widgets = data.widgets
                        )
                    </li>

                    <%-- View All Datasets --%>
                    <li>
                        <h3>
                            <a rel="contents" href="${data.viewAllDatasetsUrl}">
                                ${data.viewAllDatasetsTitle}
                            </a>
                        </h3>
                    </li>

                    <%-- Categorize Options --%>
                    <li>
                        @template.categorize_options(
                            language = data.language,
                            request = data.request,
                            loggedInAs = data.loggedInAs,
                            attributeInURL = null,
                            homePage = true,
                            tCategoryHtml = data.tCategoryHtml,
                            attributeInURLSelected = data.categorizeAttributeInURLSelected,
                            widgets = data.widgets,
                            links = data.categorizeLinks,
                            categoryList = data.categorizeList
                        )
                    </li>

                    <%-- Advanced Search --%>
                    <li>
                        <h3>
                            $unsafe{TemplateHelper.getFormated(TemplateHelper.getMessage(Message.INDEX_SEARCH_WITH, data.language), data.advancedSearchLink)}
                        </h3>
                    </li>

                    <%-- Protocol List --%>
                    <li>
                        <h3>${TemplateHelper.getMessage(Message.PROTOCOL_SEARCH_HTML, data.language)}</h3>
                        ${TemplateHelper.getMessage(Message.PROTOCOL_SEARCH_2_HTML, data.language)}
                        <br>&nbsp;
                        <table class="erd commonBGColor">
                            <tr>
                                <th>${TemplateHelper.getMessage(Message.INDEX_PROTOCOL, data.language)}</th>
                                <th>${TemplateHelper.getMessage(Message.INDEX_DESCRIPTION, data.language)}</th>
                            </tr>
                            
                            <%-- Helper template for protocol rows to avoid repetition --%>
                            @template.protocolRow(
                                url = data.erddapUrl + "/griddap/index.html",
                                protocolName = "griddap",
                                description = TemplateHelper.getMessage(Message.EDD_GRID_DAP_DESCRIPTION, data.language),
                                docUrl = data.erddapUrl + "/griddap/documentation.html",
                                data = data
                            )

                            @template.protocolRow(
                                url = data.erddapUrl + "/tabledap/index.html",
                                protocolName = "tabledap",
                                description = TemplateHelper.getMessage(Message.EDD_TABLE_DAP_DESCRIPTION, data.language),
                                docUrl = data.erddapUrl + "/tabledap/documentation.html",
                                data = data
                            )
                            
                            <%-- Files is unique due to the warning message --%>
                            <tr>
                                <td>
                                    <a rel="bookmark" href="${data.erddapUrl}/files/" 
                                       title="${TemplateHelper.getFormated(TemplateHelper.getMessage(Message.PROTOCOL_CLICK, data.language), "files")}">
                                        $unsafe{TemplateHelper.getFormated(TemplateHelper.getMessage(Message.INDEX_DATASETS, data.language), "\"files\"")}
                                    </a>
                                </td>
                                <td>
                                    ${TemplateHelper.getMessage(Message.FILES_DESCRIPTION, data.language)}
                                    ${TemplateHelper.getMessage(Message.WARNING, data.language)}
                                    ${TemplateHelper.getMessage(Message.FILES_WARNING, data.language)}
                                    <a rel="help" href="${data.erddapUrl}/files/documentation.html">
                                        $unsafe{TemplateHelper.getFormated(TemplateHelper.getMessage(Message.INDEX_DOCUMENTATION, data.language), "\"files\"")}
                                    </a>
                                </td>
                            </tr>

                            @if(TemplateHelper.getConfig().sosActive)
                                @template.protocolRow(
                                    url = data.erddapUrl + "/sos/index.html",
                                    protocolName = "SOS",
                                    description = TemplateHelper.getMessage(Message.SOS_DESCRIPTION_HTML, data.language),
                                    docUrl = data.erddapUrl + "/sos/documentation.html",
                                    data = data
                                )
                            @endif

                            @if(TemplateHelper.getConfig().wcsActive)
                                @template.protocolRow(
                                    url = data.erddapUrl + "/wcs/index.html",
                                    protocolName = "WCS",
                                    description = TemplateHelper.getMessage(Message.WCS_DESCRIPTION_HTML, data.language),
                                    docUrl = data.erddapUrl + "/wcs/documentation.html",
                                    data = data
                                )
                            @endif

                            @if(TemplateHelper.getConfig().wmsActive)
                                @template.protocolRow(
                                    url = data.erddapUrl + "/wms/index.html",
                                    protocolName = "WMS",
                                    description = TemplateHelper.getMessage(Message.WMS_DESCRIPTION_HTML, data.language),
                                    docUrl = data.erddapUrl + "/wms/documentation.html",
                                    data = data
                                )
                            @endif
                        </table>
                        &nbsp;
                    </li>

                    <%-- Developers --%>
                    <li>
                        <h3>${TemplateHelper.getMessage(Message.INDEX_DEVELOPERS_SEARCH, data.language)}</h3>
                        <ul>
                            <li><a rel="help" href="${data.erddapUrl}/rest.html">${TemplateHelper.getMessage(Message.INDEX_RESTFUL_SEARCH, data.language)}</a></li>
                            <li><a rel="help" href="${data.erddapUrl}/tabledap/allDatasets.html">${TemplateHelper.getMessage(Message.INDEX_ALL_DATASETS_SEARCH, data.language)}</a></li>
                            <li><a rel="bookmark" href="${data.erddapUrl}/opensearch1.1/index.html">${TemplateHelper.getMessage(Message.INDEX_OPEN_SEARCH, data.language)}</a></li>
                        </ul>
                    </li>

                    <%-- Multiple ERDDAPs --%>
                    <li>
                        <h3>${TemplateHelper.getMessage(Message.SEARCH_MULTIPLE_ERDDAPS, data.language)}</h3>
                        $unsafe{data.searchMultipleDescription}
                    </li>
                </ul>
                
                <p>&nbsp;<hr>

                <%-- Converters --%>
                @if(TemplateHelper.getConfig().convertersActive)
                    <p>
                        <strong>
                            <a class="selfLink" id="converters" href="#converters" rel="bookmark">${TemplateHelper.getMessage(Message.INDEX_CONVERTERS, data.language)}</a>
                        </strong>
                        <br>${TemplateHelper.getMessage(Message.INDEX_DESCRIBE_CONVERTERS, data.language)}
                        <table class="erd commonBGColor">
                            @for(var link : data.converterLinks)
                                <tr>
                                    <td><a rel="bookmark" href="${link.url}">${link.text}</a></td>
                                    <td>${link.description}</td>
                                </tr>
                            @endfor
                        </table>
                    </p>
                @endif

                <%-- Metadata --%>
                @if(TemplateHelper.getConfig().fgdcActive || TemplateHelper.getConfig().iso19115Active)
                    <p>
                        <strong>
                            <a class="selfLink" id="metadata" href="#metadata" rel="bookmark">${TemplateHelper.getMessage(Message.INDEX_METADATA, data.language)}</a>
                        </strong>
                        <br>
                        $unsafe{data.wafMessage}
                    </p>
                @endif

                <%-- Services --%>
                <p>
                    <strong>
                        <a class="selfLink" id="services" href="#services" rel="bookmark">${TemplateHelper.getMessage(Message.INDEX_SERVICES, data.language)}</a>
                    </strong>
                    <br>
                    $unsafe{TemplateHelper.getFormated(TemplateHelper.getMessage(Message.INDEX_DESCRIBE_SERVICES, data.language), data.erddapUrl)}
                </p>

                <%-- Other Features --%>
                <p>
                    <strong>
                        <a class="selfLink" id="otherFeatures" href="#otherFeatures" rel="bookmark">${TemplateHelper.getMessage(Message.OTHER_FEATURES, data.language)}</a>
                    </strong>
                    <table class="erd commonBGColor">
                        <tr>
                            <td><a rel="bookmark" href="${data.erddapUrl}/status.html">${TemplateHelper.getMessage(Message.STATUS, data.language)}</a></td>
                            <td>${TemplateHelper.getMessage(Message.STATUS_HTML, data.language)}</td>
                        </tr>
                        @if(TemplateHelper.getConfig().outOfDateDatasetsActive)
                            <tr>
                                <td><a rel="bookmark" href="${data.erddapUrl}/outOfDateDatasets.html">${TemplateHelper.getMessage(Message.OUT_OF_DATE_DATASETS, data.language)}</a></td>
                                <td>${TemplateHelper.getMessage(Message.OUT_OF_DATE_HTML, data.language)}</td>
                            </tr>
                        @endif
                        @if(TemplateHelper.getConfig().subscriptionSystemActive)
                            <tr>
                                <td><a rel="bookmark" href="${data.erddapUrl}/subscriptions/index.html">${TemplateHelper.getMessage(Message.SUBSCRIPTIONS_TITLE, data.language)}</a></td>
                                <td>${data.subscriptionsDescription}</td>
                            </tr>
                        @endif
                        @if(TemplateHelper.getConfig().slideSorterActive)
                            <tr>
                                <td><a rel="bookmark" href="${data.erddapUrl}/slidesorter.html">${TemplateHelper.getMessage(Message.SLIDE_SORTER, data.language)}</a></td>
                                <td>${TemplateHelper.getMessage(Message.SS_USE_PLAIN, data.language)}</td>
                            </tr>
                        @endif
                        @if(TemplateHelper.getConfig().dataProviderFormActive)
                            <tr>
                                <td><a rel="bookmark" href="${data.erddapUrl}/dataProviderForm.html">${TemplateHelper.getMessage(Message.DATA_PROVIDER_FORM, data.language)}</a></td>
                                <td>${TemplateHelper.getMessage(Message.DATA_PROVIDER_FORM_SHORT_DESCRIPTION, data.language)}</td>
                            </tr>
                        @endif
                    </table>
                </p>
            </p>
            </td>
        </tr>
    </table>
</div>
`)