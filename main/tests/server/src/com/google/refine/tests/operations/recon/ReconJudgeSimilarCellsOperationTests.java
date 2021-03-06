package com.google.refine.tests.operations.recon;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconJudgeSimilarCellsOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class ReconJudgeSimilarCellsOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-judge-similar-cells", ReconJudgeSimilarCellsOperation.class);
    }
    
    @Test
    public void serializeReconJudgeSimilarCellsOperation() throws Exception {
        String json = "{\"op\":\"core/recon-judge-similar-cells\","
                + "\"description\":\"Match item Unicef Indonesia (Q7884717) for cells containing \\\"UNICEF Indonesia\\\" in column organization_name\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"organization_name\","
                + "\"similarValue\":\"UNICEF Indonesia\","
                + "\"judgment\":\"matched\","
                + "\"match\":{\"id\":\"Q7884717\",\"name\":\"Unicef Indonesia\",\"score\":71.42857142857143,\"types\":[\"Q43229\"]},"
                + "\"shareNewTopics\":false}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconJudgeSimilarCellsOperation.class), json);
    }
}
