/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define(['chartist'], function (chartist) {
            return (root.returnExportsGlobal = factory(chartist));
        });
    } else if (typeof exports === 'object') {
        // Node. Does not work with strict CommonJS, but
        // only CommonJS-like enviroments that support module.exports,
        // like Node.
        module.exports = factory(require('chartist'));
    } else {
        root['Chartist.plugins.legend'] = factory(root.Chartist);
    }
}(this, function (Chartist) {
    /**
     * This Chartist plugin creates a legend to show next to the chart.
     *
     */
    'use strict';

    var defaultOptions = {
        className: '',
        classNames: false,
        removeAll: false,
        legendNames: false,
        clickable: true,
        onClick: null,
        position: 'top'
    };

    Chartist.plugins = Chartist.plugins || {};

    Chartist.plugins.legend = function (options) {

        function compareNumbers(a, b) {
            return a - b;
        }

        // Catch invalid options
        if (options && options.position) {
           if (!(options.position === 'top' || options.position === 'bottom')) {
              throw Error('The position you entered is not a valid position');
           }
        }

        options = Chartist.extend({}, defaultOptions, options);

        return function legend(chart) {
            var existingLegendElement = chart.container.querySelector('.ct-legend');
            // var existingLegendElementInParentNode = chart.container.parentNode.querySelector('.ct-legend').;

            if (existingLegendElement) {
                // Clear legend if already existing.
                existingLegendElement.parentNode.removeChild(existingLegendElement);
            }

            // if (existingLegendElementInParentNode) {
            //     // Clear legend if already existing.
            //     existingLegendElementInParentNode.parentNode.removeChild(existingLegendElementInParentNode);
            // }

            // Set a unique className for each series so that when a series is removed,
            // the other series still have the same color.
            if (options.clickable) {
                var newSeries = chart.data.series.map(function (series, seriesIndex) {
                    if (typeof series !== 'object') {
                        series = {
                            value: series
                        };
                    }

                    series.className = series.className || chart.options.classNames.series + '-' + Chartist.alphaNumerate(seriesIndex);
                    return series;
                });
                chart.data.series = newSeries;
            }

            var legendElement = document.createElement('ul'),
                isPieChart = chart instanceof Chartist.Pie;
            legendElement.className = 'ct-legend';
            if (chart instanceof Chartist.Pie) {
                legendElement.classList.add('ct-legend-inside');
            }
            if (typeof options.className === 'string' && options.className.length > 0) {
                legendElement.classList.add(options.className);
            }

            var removedSeries = [],
                originalSeries = chart.data.series.slice(0);

            // Get the right array to use for generating the legend.
            var legendNames = chart.data.series,
                useLabels = isPieChart && chart.data.labels;
            if (useLabels) {
                var originalLabels = chart.data.labels.slice(0);
                legendNames = chart.data.labels;
            }
            legendNames = options.legendNames || legendNames;

            // Check if given class names are viable to append to legends
            var classNamesViable = (Array.isArray(options.classNames) && (options.classNames.length === legendNames.length));

            // Loop through all legends to set each name in a list item.
            legendNames.forEach(function (legend, i) {
               var li = document.createElement('li');
               li.className = 'ct-series-' + i;
               // Append specific class to a legend element, if viable classes are given
               if (classNamesViable) {
                  li.className += ' ' + options.classNames[i];
               }
               li.setAttribute('data-legend', i);
               li.textContent = legend.name || legend;
               legendElement.appendChild(li);
            });

            chart.on('created', function (data) {
               // Append the legend element to the DOM
               switch (options.position) {
                  case 'top':
                     chart.container.insertBefore(legendElement, chart.container.childNodes[0]);
                     break;

                   case 'bottom':
                     chart.container.insertBefore(legendElement, null);
                     break;
               }
            });

            if (options.clickable) {
                legendElement.addEventListener('click', function (e) {
                    var li = e.target;
                    if (li.parentNode !== legendElement || !li.hasAttribute('data-legend'))
                        return;
                    e.preventDefault();

                    var seriesIndex = parseInt(li.getAttribute('data-legend')),
                        removedSeriesIndex = removedSeries.indexOf(seriesIndex);

                    if (removedSeriesIndex > -1) {
                        // Add to series again.
                        removedSeries.splice(removedSeriesIndex, 1);
                        li.classList.remove('inactive');
                    } else {
                        if (!options.removeAll) {
                             // Remove from series, only if a minimum of one series is still visible.
                          if ( chart.data.series.length > 1) {
                             removedSeries.push(seriesIndex);
                             li.classList.add('inactive');
                          }
                             // Set all series as active.
                          else {
                             removedSeries = [];
                             var seriesItems = Array.prototype.slice.call(legendElement.childNodes);
                             seriesItems.forEach(function (item) {
                                item.classList.remove('inactive');
                             });
                          }
                       }
                       else {
                          // Remove series unaffected if it is the last or not
                          removedSeries.push(seriesIndex);
                          li.classList.add('inactive');
                       }
                    }

                    // Reset the series to original and remove each series that
                    // is still removed again, to remain index order.
                    var seriesCopy = originalSeries.slice(0);
                    if (useLabels) {
                        var labelsCopy = originalLabels.slice(0);
                    }

                    // Reverse sort the removedSeries to prevent removing the wrong index.
                    removedSeries.sort(compareNumbers).reverse();

                    removedSeries.forEach(function (series) {
                        seriesCopy.splice(series, 1);
                        if (useLabels) {
                            labelsCopy.splice(series, 1);
                        }
                    });

                    if (options.onClick) {
                        options.onClick(chart, e);
                    }

                    chart.data.series = seriesCopy;
                    if (useLabels) {
                        chart.data.labels = labelsCopy;
                    }

                    chart.update();
                });
            }

        };

    };

    return Chartist.plugins.legend;

}));
