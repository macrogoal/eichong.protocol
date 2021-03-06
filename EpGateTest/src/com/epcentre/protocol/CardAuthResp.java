package com.epcentre.protocol;

public class CardAuthResp {
	
	private String innerNo;
	private String outerNo;
	int cardStatus;
	int remainAmt;
	int ret;
	int isFrozenAmt;
	public int getIsFrozenAmt() {
		return isFrozenAmt;
	}
	public void setIsFrozenAmt(int isFrozenAmt) {
		this.isFrozenAmt = isFrozenAmt;
	}

	int errorCode;
	public String getInnerNo() {
		return innerNo;
	}
	public void setInnerNo(String innerNo) {
		this.innerNo = innerNo;
	}
	public String getOuterNo() {
		return outerNo;
	}
	public void setOuterNo(String outerNo) {
		this.outerNo = outerNo;
	}
	public int getCardStatus() {
		return cardStatus;
	}
	public void setCardStatus(int cardStatus) {
		this.cardStatus = cardStatus;
	}
	public int getRemainAmt() {
		return remainAmt;
	}
	public void setRemainAmt(int remainAmt) {
		this.remainAmt = remainAmt;
	}
	public int getRet() {
		return ret;
	}
	public void setRet(int ret) {
		this.ret = ret;
	}
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	
	public CardAuthResp()
	{
		innerNo="";
		outerNo="";
		cardStatus=1;
		remainAmt=0;
		ret=1;
		errorCode=0;
		isFrozenAmt=0;
		
	}
	
	

}
