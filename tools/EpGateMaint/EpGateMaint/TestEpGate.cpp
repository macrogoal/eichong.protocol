// TestEpGate.cpp : ʵ���ļ�
//

#include "stdafx.h"
#include "EpGateMaint.h"
#include "TestEpGate.h"
#include "EpGateMaintDlg.h"




// CTestEpGate �Ի���

IMPLEMENT_DYNAMIC(CTestEpGate, CDialog)

CTestEpGate::CTestEpGate(CWnd* pParent /*=NULL*/)
	: CDialog(CTestEpGate::IDD, pParent)
{
    m_dlg = (CEpGateMaintDlg *)pParent;
}

CTestEpGate::~CTestEpGate()
{
}

void CTestEpGate::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
}


BEGIN_MESSAGE_MAP(CTestEpGate, CDialog)
	ON_BN_CLICKED(IDC_BUTTON6, &CTestEpGate::OnBnClickedButton6)
	ON_BN_CLICKED(IDC_BUTTON7, &CTestEpGate::OnBnClickedButton7)
	ON_BN_CLICKED(IDC_BUTTON8, &CTestEpGate::OnBnClickedButton8)
	ON_BN_CLICKED(IDC_BUTTON9, &CTestEpGate::OnBnClickedButton9)
	ON_BN_CLICKED(IDC_BUTTON10, &CTestEpGate::OnBnClickedButton10)
	ON_BN_CLICKED(IDC_BUTTON11, &CTestEpGate::OnBnClickedButton11)
	ON_BN_CLICKED(IDC_BUTTON12, &CTestEpGate::OnBnClickedButton12)
	ON_BN_CLICKED(IDC_BUTTON1, &CTestEpGate::OnBnClickedButton1)
	ON_BN_CLICKED(IDC_BUTTON2, &CTestEpGate::OnBnClickedButton2)
END_MESSAGE_MAP()


// CTestEpGate ��Ϣ��������

void CTestEpGate::OnBnClickedButton6()
{
	// TODO: �ڴ����ӿؼ�֪ͨ�����������
	m_dlg->OnBnClickedButton_startCharge();
}

void CTestEpGate::OnBnClickedButton7()
{
	// TODO: �ڴ����ӿؼ�֪ͨ�����������
	m_dlg->OnBnClickedButton_endCharge();
}

void CTestEpGate::OnBnClickedButton8()
{
	// TODO: �ڴ����ӿؼ�֪ͨ�����������
	m_dlg->OnBnClickedButton_startBesp();
}

void CTestEpGate::OnBnClickedButton9()
{
	// TODO: �ڴ����ӿؼ�֪ͨ�����������
	m_dlg->OnBnClickedButton_endBesp();
}

void CTestEpGate::OnBnClickedButton10()
{
	// TODO: �ڴ����ӿؼ�֪ͨ�����������
	m_dlg->OnBnClickedButton_callEp();
}

void CTestEpGate::OnBnClickedButton11()
{
	// TODO: �ڴ����ӿؼ�֪ͨ�����������
	m_dlg->OnBnClickedButton_dropClock();
}

void CTestEpGate::OnBnClickedButton12()
{
	// TODO: �ڴ����ӿؼ�֪ͨ�����������
	m_dlg->OnBnClickedButton_sendRate();
}

void CTestEpGate::OnBnClickedButton1()
{
	// TODO: �ڴ����ӿؼ�֪ͨ�����������
	m_dlg->OnBnClickedButton_createIdentyCode();
}

void CTestEpGate::OnBnClickedButton2()
{
	// TODO: �ڴ����ӿؼ�֪ͨ�����������
	m_dlg->OnBnClickedButton_stationSetEpCode();
}