from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from paddleocr import PaddleOCR
import base64
import io
from PIL import Image
import numpy as np
import os

app = FastAPI(title="BOBBuy OCR Service")

# Initialize PaddleOCR with Japanese support
# We use CPU by default to keep it compatible with most environments
ocr = PaddleOCR(lang='japan', device='cpu', enable_mkldnn=False)

class OCRRequest(BaseModel):
    image: str  # base64 encoded image

@app.post("/ocr")
async def perform_ocr(request: OCRRequest):
    try:
        # Decode base64 image
        image_data = base64.b64decode(request.image)
        image = Image.open(io.BytesIO(image_data)).convert('RGB')
        img_np = np.array(image)
        
        # Perform OCR
        result = ocr.ocr(img_np)
        
        # Format results
        output = []
        if result:
            for res in result:
                if res is None: continue
                # In PaddleOCR 3.x/Paddlex, result might be a list of dicts
                if isinstance(res, dict):
                    texts = res.get('rec_texts', [])
                    scores = res.get('rec_scores', [])
                    for t, s in zip(texts, scores):
                        output.append({"text": t, "confidence": float(s)})
                else:
                    # Legacy list-based result structure
                    for line in res:
                        text = line[1][0]
                        conf = line[1][1]
                        output.append({"text": text, "confidence": float(conf)})
        
        return {"results": output}
    except Exception as e:
        print(f"OCR Error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
