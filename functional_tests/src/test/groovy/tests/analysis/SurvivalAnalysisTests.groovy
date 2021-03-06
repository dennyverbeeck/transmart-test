package tests.analysis

import geb.Page
import geb.junit4.GebReportingTest

import junit.framework.AssertionFailedError

import org.junit.Test
import org.junit.Ignore

import functions.Constants

import pages.AnalyzePage
import pages.analyses.CoxRegressionResult
import pages.analyses.SurvivalAnalysisPage
import pages.analyses.SurvivalAnalysisSummary
import tests.CheckLoginPageAbstract

import static matchers.TableMatcher.table
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class SurvivalAnalysisTests extends CheckLoginPageAbstract{

    private void runAnalysis(Map params) {
        goToPageMaybeLogin AnalyzePage

        dragNodeToSubset params.subsetNode, 1, 1

		selectAnalysis 'Survival Analysis'
		page SurvivalAnalysisPage
		verifyAt()

		waitFor { analysisWidgetHeader }

		dragNodeToBox params.timeVariable, timeBox

		if (params.categoryVariables) {
			dragNodeToBox params.categoryVariableDragged, categoryBox,
					containsInAnyOrder(params.categoryVariables.collect { is it as String })
		} else {
			dragNodeToBox params.categoryVariableDragged, categoryBox
		}

		if (params.searchKeyword) {
            // there are some problems with timing, so wait
            // at least long enough for 2 tries
            waitFor (15, message: "SurvivalAnalysis - HighDimensional case - pathway/gene selection timed out")
                    {setSearchTarget(params)}
        }

		if (params.binningParams) {
			binning.enableBinning()

			Map binParams = params.binningParams
			if (binParams.numberOfBins) {
				binning.numberOfBins.value binParams.numberOfBins
			}
			if (binParams.autoAssignment) {
				if (binParams.autoAssignment == 'evenly spaced') {
					binning.selectEvenlySpacedBins()
				} else if (binParams.autoAssignment == 'evenly distributed') {
					binning.selectEvenlyDistributedPopulation()
				} else {
					throw new IllegalArgumentException()
				}
			}
		}

        runButton.click()
        waitFor(8, message: "SurvivalAnalysis RunButton.click() - timed out") { resultOutput } // wait up to 8 seconds for result
    }

    def setSearchTarget(params) {
        categoryHighDimButton.click()

        waitFor { highDimPopup.dialog }

        //filling the form fields is not instantaneous:
        waitFor(1) { highDimPopup.tissue }

        if (params.expectedMarkerType) {
            assertThat highDimPopup.markerType, is(params.expectedMarkerType)
        }
        if (params.expectedPlatform) {
            assertThat highDimPopup.gplPlatform, is(params.expectedPlatform)
        }
        if (params.expectedSample) {
            assertThat highDimPopup.sample, is(params.expectedSample)
        }
        if (params.expectedTissue) {
            assertThat highDimPopup.tissue, is(params.expectedTissue)
        }

        def searchTarget = params.searchKeyword

        highDimPopup.searchBox << searchTarget

        // preload
        waitFor(10) { highDimPopup.anySearchItem }

        highDimPopup.selectSearchItem searchTarget
        highDimPopup.applyButton.click()

        def probe = $('div#displaydivCategoryVariable').text()
        println("contains: " + probe.contains(searchTarget))

        probe.contains(searchTarget)

    }

    @Test
	void testClinicalVariable() {
		String sexKey     = "${Constants.GSE8581_KEY}Subjects\\Sex\\"
		def params = [
			subsetNode:              Constants.GSE8581_KEY,
			timeVariable:            "${Constants.GSE8581_KEY}Subjects\\Age\\",
			categoryVariableDragged: sexKey,
			categoryVariables:       [
				"${sexKey}male\\",
				"${sexKey}female\\"]
		]

		runAnalysis params

		/* check cox regression result */
		def allCoxRegressionResults = coxRegressionResults
		assertThat allCoxRegressionResults.size(), is(1)
		def coxRegressionData = [
			(CoxRegressionResult.NUMBER_OF_SUBJECTS_HEADER): '58',
			(CoxRegressionResult.NUMBER_OF_EVENTS_HEADER):   '58',
			(CoxRegressionResult.LIKELIHOOD_RATIO_HEADER):   '0.05 on 1 df, p=0.8177',
			(CoxRegressionResult.WALD_HEADER):               '0.05 on 1 df, p=0.8176',
			(CoxRegressionResult.LOGRANK_HEADER):            '0.05 on 1 df, p=0.8176',
		]
		assertThat allCoxRegressionResults[0], is(equalTo(coxRegressionData))

		/* check fitting summary */
		def allFittingSummaries = fittingSummaries
		assertThat allFittingSummaries.size(), is(1)

		def fittingSummaryRowHeaders = ['female', 'male']
		def fittingSummaryData =
				[
					[30, 30, 30, 30, 65, 61, 72],
					//female
					[28, 28, 28, 28, 68, 63, 73]]  //male
		assertThat allFittingSummaries[0], is(table(
				fittingSummaryRowHeaders,
				SurvivalAnalysisSummary.ALL_HEADERS,
				fittingSummaryData))
	}

    @Ignore
    @Test
	void testMrnaCategoryEvenlySpaced() {
		String sexKey     = "${Constants.GSE8581_KEY}Subjects\\Sex\\"

		def highDimExpectations = [
			expectedMarkerType: 'Gene Expression',
			expectedPlatform:   'GPL570',
			expectedSample:     'Human',
			expectedTissue:     'Lung',
		]

		def binningParams = [
			numberOfBins:       2,
			autoAssignment:     'evenly spaced'
		]

		def params = [
			subsetNode:              Constants.GSE8581_KEY,
			timeVariable:            "${Constants.GSE8581_KEY}Subjects\\Age\\",
            categoryVariableDragged: "${Constants.GSE8581_KEY}MRNA\\Biomarker Data\\GPL570\\Lung\\",
			searchKeyword:           'TP53',
			*:                       highDimExpectations,
			binningParams:           binningParams,
		]

		runAnalysis params

		/* TODO: final assertions missing! */
		assertThat(0,0)
	}
}
