
/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

class Chart extends React.Component{

    componentDidMount() {
        this.updateChart(this.props.chartData);
    };

    updateChart() {
        let chartName = this.props.chartName;
        let options = this.optionsSetter();
        let newChart = new Chartist.Line('#' + chartName, this.props.chartData, options);
        // newChart.on('draw', function(data) {
        //     if(data.type === 'grid' && data.index !== 0) {
        //         data.element.remove();
        //     }});

        return newChart;
    };

    shouldComponentUpdate() {
        return true;
    };

    componentDidUpdate(){
        this.updateChart(this.props.chartData);
    }

    optionsSetter() {
        let options = new Object();
        options.fullWidth = true;
        options.width = "100%";
        options.height = "85%";
        options.stretch = true;
        options.chartPadding = {left: 40, right: 20,bottom: 10};
        options.plugins = [];
        options.plugins.push(Chartist.plugins.tooltip({
            anchorToPoint: true,
            appendToBody: false,
            tooltipOffset: {
                x: 0,
                y: 40
            },
            transformTooltipTextFnc: function(value)
            {
                return Chartist.roundWithPrecision(value, 3);
            }
        }));
        options.plugins.push(Chartist.plugins.ctAxisTitle({
            axisX: {
                axisTitle: this.props.chartData.x_axis_title,
                axisClass: 'ct-axis-title',
                offset: {
                    x: 0,
                    y: 40
                },
                textAnchor: 'middle'
            },
            axisY: {
                axisTitle: this.props.chartData.y_axis_title,
                axisClass: 'ct-axis-title-y',
                offset: {
                    x: 0,
                    y: 0
                },
                textAnchor: 'middle',
                flipTitle: false
            }
        }));
        if ((this.props.multiSeriesChart === true)) {
            options.plugins.push(Chartist.plugins.legend({
                position: 'bottom'
            }));
        }
        options.lineSmooth = Chartist.Interpolation.step();
        options.axisY = {
            labelInterpolationFnc: function(value)
            {
                return Chartist.roundWithPrecision(value, 6);
            }
        }
        return options;
    };

    componentWillReceiveProps(newProps) {
        this.updateChart(newProps);
    }

    componentWillUnmount() {
        if (this.chartist) {
            try {
                this.chartist.detach();
            } catch (err) {
                throw new Error('Internal chartist error', err);
            }
        }
    }

    render() {
        return (
               <div id={this.props.chartName} className='ct-chart'>
               </div>
       );
    }
};

class Charts extends React.Component
{
    render() {
        let graphsData = this.props.graphsData;
        let charts = Object.keys(graphsData).map((chartName, index)=>
        {
            let chartData = graphsData[chartName];
            let multiSeriesChart = false;
            if(chartData.series.length > 1)
            {
                multiSeriesChart = true;
            }
            let chartBoxKey = chartName + "box";
            let chartAreaKey = chartName + "Area";
            return (

                <div key = {chartAreaKey} className="ct-chartBox">
                <div>
                    <span className="ct-chart-title">{chartData.title}</span>
                    <br className="ct-chart-seprator"/>
                    <span className="ct-chart-desc-title">Description: </span>
                    <span className="ct-chart-desc">{chartData.description}</span>
                </div>
                    <Chart key = {chartName} chartName = {chartName} chartData = {chartData}
                           multiSeriesChart = {multiSeriesChart} {...this.props}/>

                </div>

            );
        });
        let returnValue = <div id="charts"> </div>;
        if(charts.length > 0)
        {
            returnValue = <div id="charts">{charts}</div>;
        }
        return returnValue;
    }
};



class ChartDashboard extends React.Component{
    static propTypes: {
        globalOptions: React.PropTypes.array,
        graphsData: React.PropTypes.object.isRequired,
        dataProcessFunc: React.PropTypes.func,
        multiSeries: React.PropTypes.boolean,
    };

    static defaultProps : {
        globalOptions: [],
        graphsData: {},
        multiSeries: false,
    };

    render() {
        return (<Charts {...this.props}/> );
    }
};

/**
 * Checks if the given graphs is of SLA rule type that has multiple transaction and therefore
 * requires legend and multiple series support
 * @param graphKey - the graph name (as defined in the data object given.
 * @returns boolean iff it's of multiple transaction type.
 */
function isMultipleTransactionGraph(graphKey) {
    if(graphKey == 'averageTransactionResponseTime' || graphKey == 'percentileTransaction')
    {
        return true;
    }

    return false;
};

/**
 * Updates the graph view per scenario key
 * @param scenarioKey - the selected scenario
 */
function updateGraphs(scenarioKey)
{
    instance.getGraphData(function(t)
    {
        let graphsData = (t.responseObject())[scenarioKey];
        ReactDOM.render(<ChartDashboard graphsData = {graphsData.scenarioData} dataProcessFunc = {isMultipleTransactionGraph}/>
            ,document.querySelector('.graphCon'));
        // ReactDOM.render(<ScenarioTable scenName = {scenarioKey} scenData = {graphsData.scenarioStats}/>,
        //     document.querySelector('.scenarioSummary'));
    });
};

var Dropdown = React.createClass({

    propTypes: {
        id: React.PropTypes.string.isRequired,
        options: React.PropTypes.array.isRequired,
        value: React.PropTypes.oneOfType(
            [
                React.PropTypes.object,
                React.PropTypes.string
            ]
        ),
        valueField: React.PropTypes.string,
        labelField: React.PropTypes.string,
        onChange: React.PropTypes.func
    },

    getDefaultProps: function() {
        return {
            value: null,
            valueField: 'value',
            labelField: 'label',
            onChange: null
        };
    },

    getInitialState: function() {
        var selected = this.getSelectedFromProps(this.props);
        // console.log(JSON.stringify(selected));
        updateGraphs(selected);
        return {
            selected: selected
        }
    },

    componentWillReceiveProps: function(nextProps) {
        var selected = this.getSelectedFromProps(nextProps);
        this.setState({
            selected: selected
        });
    },

    getSelectedFromProps(props) {
        var selected;
        if (props.value === null && props.options.length !== 0) {
            selected = props.options[0][props.valueField];
        } else {
            selected = props.value;
        }
        return selected;
    },

    render: function() {
        var self = this;
        var options = self.props.options.map(function(option) {
            return (
                <option key={option[self.props.valueField]} value={option[self.props.valueField]}>
                    Scenario name: {option[self.props.labelField]}
                </option>
            )
        });
        return (
            <select id={this.props.id}
                    className='st-dropdown'
                    value={this.state.selected}
                    onChange={this.handleChange}>
                {options}
            </select>
        );
    },

    handleChange: function(e) {
        if (this.props.onChange) {
            var change = {
                oldValue: this.state.selected,
                newValue: e.target.value
            };
            this.props.onChange(change);
        }
        this.setState({selected: e.target.value});
    }

});

var dropDownOnChange = function(change) {
    updateGraphs(change.newValue);
};

instance.getScenarioList(function(t)
{
    let tData = t.responseObject();
    ReactDOM.render(<div>
            {/*<span>*/}
                {/*Scenario name:*/}
            {/*</span>*/}
            <Dropdown id='myDropdown' options= {tData} labelField='ScenarioName' valueField='ScenarioName' onChange={dropDownOnChange}/>
        </div>, document.getElementById('scenarioDropDown'));
});

class ScenarioTable extends React.Component{
    static propTypes: {
        scenName: React.PropTypes.string.isRequired,
        scenData: React.PropTypes.array.isRequired,
    };

    render() {
        const tableId = this.props.scenarioKey + '_table'
        const time = this.props.scenData.AvgScenarioDuration.AvgDuration;

        const days = Math.floor(time / 86400);
        const hours = Math.floor((time - (86400 * days)) / 3600);
        const minutes = Math.floor((time - (3600 * hours)) / 60);
        const seconds = Math.floor(time - (60 * minutes));

        return (<div id={this.props.scenarioKey} className='scenario-stats'>
            <table className="st-table">
                <thead className="st-table-head">
                    <tr className="st-table-row">
                        <th className="st-table-header">
                            Average duration
                            (Days:Hours:Min:Sec)
                        </th>
                        <th className="st-table-header">
                            Average total transaction state
                        </th>
                        <th className="st-table-header">
                            Average maximum connections
                        </th>
                        <th className="st-table-header">
                            Average maximum VUsers
                        </th>
                    </tr>
                </thead>
                <tbody className="st-table-body">
                    <tr className="st-table-row">
                        <td className="st-table-cell">
                            {days}:{hours}:{minutes}:{seconds}
                        </td>
                        <td className = "st-table-cell">
                            <td className = "st-table-inner-cell">
                                <img src="/plugin/hp-application-automation-tools-plugin/icons/16x16/passed.png" alt="Passed:"/>
                                {this.props.scenData.TransactionSummary.Pass} Passed
                            </td>
                            <td className = "st-table-inner-cell">
                                <img src="/plugin/hp-application-automation-tools-plugin/icons/16x16/stop.png" alt="Stopped:"/>
                                {this.props.scenData.TransactionSummary.Stop} Stopped
                            </td>
                            <td className = "st-table-inner-cell">
                                <img src="/plugin/hp-application-automation-tools-plugin/icons/16x16/failed.png" alt="Failed:"/>
                                {this.props.scenData.TransactionSummary.Fail} Failed
                            </td>
                        </td>
                        <td className="st-table-cell">
                            {this.props.scenData.AvgMaxConnections.AvgMaxConnection}
                        </td>
                        <td className="st-table-cell">
                            {this.props.scenData.VUserSummary.AvgMaxVuser}
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>);
    }
};