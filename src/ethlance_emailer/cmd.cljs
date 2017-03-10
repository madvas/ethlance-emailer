(ns ethlance-emailer.cmd
  (:require
    [cljs-web3.utils :as web3-utils]
    [cljs.nodejs :as nodejs]
    [cljs.spec :as s]
    [ethlance-emailer.ethlance-db :as ethlance-db]
    [ethlance-emailer.sendgrid :as sendgrid]
    [ethlance-emailer.templates :as templates]
    [ethlance-emailer.utils :as u]
    [ethlance-emailer.web3 :as web3]
    [goog.string.format]
    [medley.core :as medley]
    [print.foo :include-macros true]
    [ethlance-emailer.constants :as constants]))

(nodejs/enable-util-print!)

(def Web3 (js/require "web3"))
(def schedule (js/require "node-schedule"))

(def web3 (web3/create-web3 Web3 (or (aget nodejs/process "env" "WEB3_URL") "http://localhost:8549")))

(def contracts
  {:ethlance-db {:name "EthlanceDB" :address "0x5371a8d8d8a86c76de935821ad1a3e9b908cfced"}
   :ethlance-user {:name "EthlanceUser" :setter? true :address "0x85c1b0dc9e3443e06e5f1b09844631378825bb14"}
   :ethlance-contract {:name "EthlanceContract" :setter? true :address "0x9d0aba974c3158cc9fd9a530acd83a3ff7c14964"}
   :ethlance-job {:name "EthlanceJob" :setter? true :address "0x3d3bb143a6ee72deb9646c14b403ccc3f6e3c2c8"}
   :ethlance-invoice {:name "EthlanceInvoice" :setter? true :address "0x917db76c206f744274375428e261fa6521ac1b05"}
   :ethlance-search {:name "EthlanceSearch" :address "0x8f61f16b154d676b05ac03ac1659df3c1e1b7916"}
   :ethlance-message {:name "EthlanceSearch" :address "0x3d4fc3a6fb3186efae7087f74dd489d90980b5ac"}})

(def abis
  {:ethlance-job "[{\"constant\":false,\"inputs\":[{\"name\":\"jobId\",\"type\":\"uint256\"},{\"name\":\"status\",\"type\":\"uint8\"}],\"name\":\"setJobStatus\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getSenderUserId\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"smartContractStatus\",\"outputs\":[{\"name\":\"\",\"type\":\"uint8\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"ethlanceDB\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"key\",\"type\":\"bytes32\"}],\"name\":\"getConfig\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"owner\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"description\",\"type\":\"string\"},{\"name\":\"skills\",\"type\":\"uint256[]\"},{\"name\":\"language\",\"type\":\"uint256\"},{\"name\":\"budget\",\"type\":\"uint256\"},{\"name\":\"uint8Items\",\"type\":\"uint8[]\"}],\"name\":\"addJob\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"jobId\",\"type\":\"uint256\"}],\"name\":\"setJobHiringDone\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_newOwner\",\"type\":\"address\"}],\"name\":\"changeOwner\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_status\",\"type\":\"uint8\"}],\"name\":\"setSmartContractStatus\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"inputs\":[{\"name\":\"_ethlanceDB\",\"type\":\"address\"}],\"payable\":false,\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"jobId\",\"type\":\"uint256\"}],\"name\":\"onJobAdded\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"status\",\"type\":\"uint8\"}],\"name\":\"onSmartContractStatusSet\",\"type\":\"event\"}]"
   :ethlance-user "[{\"constant\":false,\"inputs\":[],\"name\":\"getSenderUserId\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"smartContractStatus\",\"outputs\":[{\"name\":\"\",\"type\":\"uint8\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"isAvailable\",\"type\":\"bool\"},{\"name\":\"jobTitle\",\"type\":\"string\"},{\"name\":\"hourlyRate\",\"type\":\"uint256\"},{\"name\":\"hourlyRateCurrency\",\"type\":\"uint8\"},{\"name\":\"categories\",\"type\":\"uint256[]\"},{\"name\":\"skills\",\"type\":\"uint256[]\"},{\"name\":\"description\",\"type\":\"string\"}],\"name\":\"setFreelancer\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"userId\",\"type\":\"uint256\"},{\"name\":\"status\",\"type\":\"uint8\"}],\"name\":\"setUserStatus\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"ethlanceDB\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"key\",\"type\":\"bytes32\"}],\"name\":\"getConfig\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"email\",\"type\":\"string\"},{\"name\":\"gravatar\",\"type\":\"bytes32\"},{\"name\":\"country\",\"type\":\"uint256\"},{\"name\":\"state\",\"type\":\"uint256\"},{\"name\":\"languages\",\"type\":\"uint256[]\"},{\"name\":\"github\",\"type\":\"string\"},{\"name\":\"linkedin\",\"type\":\"string\"},{\"name\":\"description\",\"type\":\"string\"}],\"name\":\"registerEmployer\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"owner\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"description\",\"type\":\"string\"}],\"name\":\"setEmployer\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_newOwner\",\"type\":\"address\"}],\"name\":\"changeOwner\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_status\",\"type\":\"uint8\"}],\"name\":\"setSmartContractStatus\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"email\",\"type\":\"string\"},{\"name\":\"gravatar\",\"type\":\"bytes32\"},{\"name\":\"country\",\"type\":\"uint256\"},{\"name\":\"state\",\"type\":\"uint256\"},{\"name\":\"languages\",\"type\":\"uint256[]\"},{\"name\":\"github\",\"type\":\"string\"},{\"name\":\"linkedin\",\"type\":\"string\"},{\"name\":\"isAvailable\",\"type\":\"bool\"},{\"name\":\"jobTitle\",\"type\":\"string\"},{\"name\":\"hourlyRate\",\"type\":\"uint256\"},{\"name\":\"hourlyRateCurrency\",\"type\":\"uint8\"},{\"name\":\"categories\",\"type\":\"uint256[]\"},{\"name\":\"skills\",\"type\":\"uint256[]\"},{\"name\":\"description\",\"type\":\"string\"}],\"name\":\"registerFreelancer\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"email\",\"type\":\"string\"},{\"name\":\"gravatar\",\"type\":\"bytes32\"},{\"name\":\"country\",\"type\":\"uint256\"},{\"name\":\"state\",\"type\":\"uint256\"},{\"name\":\"languages\",\"type\":\"uint256[]\"},{\"name\":\"github\",\"type\":\"string\"},{\"name\":\"linkedin\",\"type\":\"string\"}],\"name\":\"setUser\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"inputs\":[{\"name\":\"_ethlanceDB\",\"type\":\"address\"}],\"payable\":false,\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"userId\",\"type\":\"uint256\"}],\"name\":\"onFreelancerAdded\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"userId\",\"type\":\"uint256\"}],\"name\":\"onEmployerAdded\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"status\",\"type\":\"uint8\"}],\"name\":\"onSmartContractStatusSet\",\"type\":\"event\"}]"
   :ethlance-contract "[{\"constant\":false,\"inputs\":[{\"name\":\"contractId\",\"type\":\"uint256\"},{\"name\":\"feedback\",\"type\":\"string\"},{\"name\":\"rating\",\"type\":\"uint8\"}],\"name\":\"addJobContractFeedback\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"contractId\",\"type\":\"uint256\"},{\"name\":\"description\",\"type\":\"string\"},{\"name\":\"isHiringDone\",\"type\":\"bool\"}],\"name\":\"addJobContract\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getSenderUserId\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"smartContractStatus\",\"outputs\":[{\"name\":\"\",\"type\":\"uint8\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"ethlanceDB\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"key\",\"type\":\"bytes32\"}],\"name\":\"getConfig\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"owner\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"jobId\",\"type\":\"uint256\"},{\"name\":\"description\",\"type\":\"string\"},{\"name\":\"rate\",\"type\":\"uint256\"}],\"name\":\"addJobProposal\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_newOwner\",\"type\":\"address\"}],\"name\":\"changeOwner\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"contractId\",\"type\":\"uint256\"},{\"name\":\"description\",\"type\":\"string\"}],\"name\":\"cancelJobContract\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_status\",\"type\":\"uint8\"}],\"name\":\"setSmartContractStatus\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"jobId\",\"type\":\"uint256\"},{\"name\":\"freelancerId\",\"type\":\"uint256\"},{\"name\":\"description\",\"type\":\"string\"}],\"name\":\"addJobInvitation\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"inputs\":[{\"name\":\"_ethlanceDB\",\"type\":\"address\"}],\"payable\":false,\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"contractId\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"employerId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"freelancerId\",\"type\":\"uint256\"}],\"name\":\"onJobProposalAdded\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"contractId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"employerId\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"freelancerId\",\"type\":\"uint256\"}],\"name\":\"onJobContractAdded\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"contractId\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"employerId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"freelancerId\",\"type\":\"uint256\"}],\"name\":\"onJobContractCancelled\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"contractId\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"receiverId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"senderId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"isSenderFreelancer\",\"type\":\"bool\"}],\"name\":\"onJobContractFeedbackAdded\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"jobId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"contractId\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"freelancerId\",\"type\":\"uint256\"}],\"name\":\"onJobInvitationAdded\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"status\",\"type\":\"uint8\"}],\"name\":\"onSmartContractStatusSet\",\"type\":\"event\"}]"
   :ethlance-invoice "[{\"constant\":false,\"inputs\":[{\"name\":\"contractId\",\"type\":\"uint256\"},{\"name\":\"description\",\"type\":\"string\"},{\"name\":\"uintArgs\",\"type\":\"uint256[]\"}],\"name\":\"addInvoice\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getSenderUserId\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"smartContractStatus\",\"outputs\":[{\"name\":\"\",\"type\":\"uint8\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"ethlanceDB\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"key\",\"type\":\"bytes32\"}],\"name\":\"getConfig\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"owner\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_newOwner\",\"type\":\"address\"}],\"name\":\"changeOwner\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"invoiceId\",\"type\":\"uint256\"}],\"name\":\"payInvoice\",\"outputs\":[],\"payable\":true,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_status\",\"type\":\"uint8\"}],\"name\":\"setSmartContractStatus\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"invoiceId\",\"type\":\"uint256\"}],\"name\":\"cancelInvoice\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"inputs\":[{\"name\":\"_ethlanceDB\",\"type\":\"address\"}],\"payable\":false,\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"invoiceId\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"employerId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"freelancerId\",\"type\":\"uint256\"}],\"name\":\"onInvoiceAdded\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"invoiceId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"employerId\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"freelancerId\",\"type\":\"uint256\"}],\"name\":\"onInvoicePaid\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"invoiceId\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"employerId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"freelancerId\",\"type\":\"uint256\"}],\"name\":\"onInvoiceCancelled\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"status\",\"type\":\"uint8\"}],\"name\":\"onSmartContractStatusSet\",\"type\":\"event\"}]"
   :ethlance-config "[{\"constant\":true,\"inputs\":[{\"name\":\"keys\",\"type\":\"bytes32[]\"}],\"name\":\"getConfigs\",\"outputs\":[{\"name\":\"values\",\"type\":\"uint256[]\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getSenderUserId\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"smartContractStatus\",\"outputs\":[{\"name\":\"\",\"type\":\"uint8\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"keys\",\"type\":\"bytes32[]\"},{\"name\":\"vals\",\"type\":\"uint256[]\"}],\"name\":\"setConfigs\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"skillId\",\"type\":\"uint256\"},{\"name\":\"name\",\"type\":\"bytes32\"}],\"name\":\"setSkillName\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"ethlanceDB\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"key\",\"type\":\"bytes32\"}],\"name\":\"getConfig\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"owner\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"skillIds\",\"type\":\"uint256[]\"}],\"name\":\"blockSkills\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_newOwner\",\"type\":\"address\"}],\"name\":\"changeOwner\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_status\",\"type\":\"uint8\"}],\"name\":\"setSmartContractStatus\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"names\",\"type\":\"bytes32[]\"}],\"name\":\"addSkills\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"inputs\":[{\"name\":\"_ethlanceDB\",\"type\":\"address\"}],\"payable\":false,\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"skillIds\",\"type\":\"uint256[]\"}],\"name\":\"onSkillsAdded\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"skillIds\",\"type\":\"uint256[]\"}],\"name\":\"onSkillsBlocked\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"skillId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"name\",\"type\":\"bytes32\"}],\"name\":\"onSkillNameSet\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"keys\",\"type\":\"bytes32[]\"}],\"name\":\"onConfigsChanged\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"status\",\"type\":\"uint8\"}],\"name\":\"onSmartContractStatusSet\",\"type\":\"event\"}]"
   :ethlance-db "[{\"constant\":true,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"getBytes32Value\",\"outputs\":[{\"name\":\"\",\"type\":\"bytes32\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"deleteAddressValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"value\",\"type\":\"uint8\"}],\"name\":\"setUInt8Value\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"deleteBytesValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"deleteBytes32Value\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"getBooleanValue\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"types\",\"type\":\"uint8[]\"}],\"name\":\"getUIntTypesCount\",\"outputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"value\",\"type\":\"bytes32\"}],\"name\":\"setBytes32Value\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"x\",\"type\":\"bool\"}],\"name\":\"booleanToUInt\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"setUIntValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"deleteBooleanValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"value\",\"type\":\"bool\"}],\"name\":\"setBooleanValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"getBytesValue\",\"outputs\":[{\"name\":\"\",\"type\":\"bytes\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"getAddressValue\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"allowedContractsCount\",\"outputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"}],\"name\":\"allowedContracts\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"getAllowedContracts\",\"outputs\":[{\"name\":\"addresses\",\"type\":\"address[]\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"value\",\"type\":\"address\"}],\"name\":\"setAddressValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"subUIntValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"getUInt8Value\",\"outputs\":[{\"name\":\"\",\"type\":\"uint8\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"deleteIntValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"deleteUInt8Value\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addresses\",\"type\":\"address[]\"}],\"name\":\"removeAllowedContracts\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"owner\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"getIntValue\",\"outputs\":[{\"name\":\"\",\"type\":\"int256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"deleteUIntValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"allowedContractsKeys\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"getStringValue\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_newOwner\",\"type\":\"address\"}],\"name\":\"changeOwner\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"value\",\"type\":\"int256\"}],\"name\":\"setIntValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"deleteStringValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"}],\"name\":\"getUIntValue\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"addUIntValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"uintType\",\"type\":\"uint8\"}],\"name\":\"getUIntValueConverted\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"value\",\"type\":\"bytes\"}],\"name\":\"setBytesValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"records\",\"type\":\"bytes32[]\"},{\"name\":\"types\",\"type\":\"uint8[]\"}],\"name\":\"getEntityList\",\"outputs\":[{\"name\":\"items\",\"type\":\"uint256[]\"},{\"name\":\"strs\",\"type\":\"string\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"record\",\"type\":\"bytes32\"},{\"name\":\"value\",\"type\":\"string\"}],\"name\":\"setStringValue\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addresses\",\"type\":\"address[]\"}],\"name\":\"addAllowedContracts\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"type\":\"constructor\"}]"
   :ethlance-search "[{\"constant\":true,\"inputs\":[],\"name\":\"ethlanceDB\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"categoryId\",\"type\":\"uint256\"},{\"name\":\"skillsAnd\",\"type\":\"uint256[]\"},{\"name\":\"skillsOr\",\"type\":\"uint256[]\"},{\"name\":\"paymentTypes\",\"type\":\"uint8[]\"},{\"name\":\"experienceLevels\",\"type\":\"uint8[]\"},{\"name\":\"estimatedDurations\",\"type\":\"uint8[]\"},{\"name\":\"hoursPerWeeks\",\"type\":\"uint8[]\"},{\"name\":\"minBudgets\",\"type\":\"uint256[]\"},{\"name\":\"uintArgs\",\"type\":\"uint256[]\"}],\"name\":\"searchJobs\",\"outputs\":[{\"name\":\"jobIds\",\"type\":\"uint256[]\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"categoryId\",\"type\":\"uint256\"},{\"name\":\"skillsAnd\",\"type\":\"uint256[]\"},{\"name\":\"skillsOr\",\"type\":\"uint256[]\"},{\"name\":\"minAvgRating\",\"type\":\"uint8\"},{\"name\":\"minRatingsCount\",\"type\":\"uint256\"},{\"name\":\"minHourlyRates\",\"type\":\"uint256[]\"},{\"name\":\"maxHourlyRates\",\"type\":\"uint256[]\"},{\"name\":\"uintArgs\",\"type\":\"uint256[]\"}],\"name\":\"searchFreelancers\",\"outputs\":[{\"name\":\"userIds\",\"type\":\"uint256[]\"}],\"payable\":false,\"type\":\"function\"},{\"inputs\":[{\"name\":\"_ethlanceDB\",\"type\":\"address\"}],\"payable\":false,\"type\":\"constructor\"}]"
   :ethlance-message "[{\"constant\":false,\"inputs\":[],\"name\":\"getSenderUserId\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"smartContractStatus\",\"outputs\":[{\"name\":\"\",\"type\":\"uint8\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"ethlanceDB\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"key\",\"type\":\"bytes32\"}],\"name\":\"getConfig\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"contractId\",\"type\":\"uint256\"},{\"name\":\"message\",\"type\":\"string\"}],\"name\":\"addJobContractMessage\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"owner\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_newOwner\",\"type\":\"address\"}],\"name\":\"changeOwner\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_status\",\"type\":\"uint8\"}],\"name\":\"setSmartContractStatus\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"inputs\":[{\"name\":\"_ethlanceDB\",\"type\":\"address\"}],\"payable\":false,\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"messageId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"contractId\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"receiverId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"senderId\",\"type\":\"uint256\"}],\"name\":\"onJobContractMessageAdded\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"status\",\"type\":\"uint8\"}],\"name\":\"onSmartContractStatusSet\",\"type\":\"event\"}]"})


(def instances
  (into {}
        (for [[contract-key {:keys [:address]}] contracts]
          {contract-key (web3/contract-at web3
                                          (js/JSON.parse (get abis contract-key))
                                          address)})))

(defn setup-listener! [contract-key fn-key callback]
  (web3/contract-call (get instances contract-key)
                      fn-key
                      {}
                      "latest"
                      (fn [err res]
                        (if err
                          (println "ERROR: " err)
                          (callback (:args res))))))

(defn on-invoice-added [{:keys [:invoice-id :employer-id :freelancer-id]}]
  (let [invoice-id (u/big-num->num invoice-id)
        employer-id (u/big-num->num employer-id)
        employer (ethlance-db/get-user employer-id instances [:user.notif/disabled-all?
                                                              :user.notif/disabled-on-invoice-added?])
        freelancer (ethlance-db/get-user (u/big-num->num freelancer-id) instances)
        invoice (ethlance-db/get-invoice invoice-id instances)
        body (templates/on-invoice-added invoice freelancer)]
    (when (and (not (:user.notif/disabled-all? employer))
               (not (:user.notif/disabled-on-invoice-added? employer)))
      (sendgrid/send-notification-mail employer-id
                                       (:user/email employer)
                                       "You received an invoice to pay"
                                       body
                                       (:user/name employer)
                                       "Open Invoice"
                                       (u/full-path-for :invoice/detail :invoice/id invoice-id)
                                       :on-invoice-added))))

(defn on-invoice-paid [{:keys [:invoice-id :employer-id :freelancer-id]}]
  (let [invoice-id (u/big-num->num invoice-id)
        freelancer-id (u/big-num->num freelancer-id)
        employer (ethlance-db/get-user (u/big-num->num employer-id) instances)
        freelancer (ethlance-db/get-user freelancer-id instances [:user.notif/disabled-all?
                                                                  :user.notif/disabled-on-invoice-paid?])
        invoice (ethlance-db/get-invoice invoice-id instances [:invoice/amount])
        body (templates/on-invoice-paid invoice employer)]
    (when (and (not (:user.notif/disabled-all? freelancer))
               (not (:user.notif/disabled-on-invoice-paid? freelancer)))
      (sendgrid/send-notification-mail freelancer-id
                                       (:user/email freelancer)
                                       "Your invoice was paid"
                                       body
                                       (:user/name freelancer)
                                       "Open Invoice"
                                       (u/full-path-for :invoice/detail :invoice/id invoice-id)
                                       :on-invoice-paid))))

(defn on-invoice-cancelled [{:keys [:invoice-id :employer-id :freelancer-id]}]
  (let [invoice-id (u/big-num->num invoice-id)
        employer-id (u/big-num->num employer-id)
        employer (ethlance-db/get-user employer-id instances [:user.notif/disabled-all?
                                                              :user.notif/disabled-on-invoice-added?])
        freelancer (ethlance-db/get-user (u/big-num->num freelancer-id) instances)
        invoice (ethlance-db/get-invoice invoice-id instances [:invoice/amount])
        body (templates/on-invoice-cancelled invoice freelancer)]
    (when (and (not (:user.notif/disabled-all? employer))
               (not (:user.notif/disabled-on-invoice-added? employer)))
      (sendgrid/send-notification-mail employer-id
                                       (:user/email employer)
                                       "Invoice you previously received was cancelled"
                                       body
                                       (:user/name employer)
                                       "Open Invoice"
                                       (u/full-path-for :invoice/detail :invoice/id invoice-id)
                                       :on-invoice-cancelled))))

(defn on-job-proposal-added [{:keys [:contract-id :employer-id :freelancer-id] :as args}]
  (let [contract-id (u/big-num->num contract-id)
        employer-id (u/big-num->num employer-id)
        contract (ethlance-db/get-contract contract-id [:contract/job :proposal/description :proposal/rate] instances)
        job-id (u/big-num->num (:contract/job contract))
        job (ethlance-db/get-job job-id instances)
        freelancer (ethlance-db/get-user (u/big-num->num freelancer-id) instances)
        employer (ethlance-db/get-user employer-id instances [:user.notif/disabled-all?
                                                              :user.notif/disabled-on-job-proposal-added?])
        body (templates/on-job-proposal-added job contract freelancer)]
    (when (and (not (:user.notif/disabled-all? employer))
               (not (:user.notif/disabled-on-job-proposal-added? employer)))
      (sendgrid/send-notification-mail employer-id
                                       (:user/email employer)
                                       "Your job received a proposal"
                                       body
                                       (:user/name employer)
                                       "Open Proposal"
                                       (u/full-path-for :contract/detail :contract/id contract-id)
                                       :on-job-proposal-added))))

(defn on-job-contract-added [{:keys [:contract-id :employer-id :freelancer-id]}]
  (let [contract-id (u/big-num->num contract-id)
        freelancer-id (u/big-num->num freelancer-id)
        contract (ethlance-db/get-contract contract-id [:contract/job :contract/description] instances)
        job-id (u/big-num->num (:contract/job contract))
        job (ethlance-db/get-job job-id instances)
        freelancer (ethlance-db/get-user freelancer-id instances [:user.notif/disabled-all?
                                                                  :user.notif/disabled-on-job-contract-added?])
        body (templates/on-job-contract-added job contract)]
    (when (and (not (:user.notif/disabled-all? freelancer))
               (not (:user.notif/disabled-on-job-contract-added? freelancer)))
      (sendgrid/send-notification-mail freelancer-id
                                       (:user/email freelancer)
                                       "You got hired!"
                                       body
                                       (:user/name freelancer)
                                       "Open Contract"
                                       (u/full-path-for :contract/detail :contract/id contract-id)
                                       :on-job-contract-added))))

(defn on-job-contract-cancelled [{:keys [:contract-id :employer-id :freelancer-id] :as args}]
  (let [contract-id (u/big-num->num contract-id)
        employer-id (u/big-num->num employer-id)
        contract (ethlance-db/get-contract contract-id [:contract/job :contract/cancel-description] instances)
        job-id (u/big-num->num (:contract/job contract))
        job (ethlance-db/get-job job-id instances)
        freelancer (ethlance-db/get-user (u/big-num->num freelancer-id) instances)
        employer (ethlance-db/get-user (u/big-num->num employer-id) instances [:user.notif/disabled-all?
                                                                               :user.notif/disabled-on-job-proposal-added?])
        body (templates/on-job-contract-cancelled job contract freelancer)]
    (when (and (not (:user.notif/disabled-all? employer))
               (not (:user.notif/disabled-on-job-proposal-added? employer)))
      (sendgrid/send-notification-mail employer-id
                                       (:user/email employer)
                                       "Freelancer cancelled your contract"
                                       body
                                       (:user/name employer)
                                       "Open Contract"
                                       (u/full-path-for :contract/detail :contract/id contract-id)
                                       :on-job-contract-cancelled))))

(defn on-job-contract-feedback-added [{:keys [:contract-id :receiver-id :sender-id :is-sender-freelancer]}]
  (let [contract-id (u/big-num->num contract-id)
        fields (if is-sender-freelancer
                 [:contract/freelancer-feedback-rating :contract/freelancer-feedback]
                 [:contract/employer-feedback-rating :contract/employer-feedback])
        contract (ethlance-db/get-contract contract-id fields instances)
        sender (ethlance-db/get-user (u/big-num->num sender-id) instances)
        receiver-id (u/big-num->num receiver-id)
        receiver (ethlance-db/get-user receiver-id instances [:user.notif/disabled-all?
                                                              :user.notif/disabled-on-job-contract-feedback-added?])
        body (templates/on-job-contract-feedback-added
               (get contract (if is-sender-freelancer :contract/freelancer-feedback-rating
                                                      :contract/employer-feedback-rating))
               (get contract (if is-sender-freelancer :contract/freelancer-feedback
                                                      :contract/employer-feedback))
               sender)]
    (when (and (not (:user.notif/disabled-all? receiver))
               (not (:user.notif/disabled-on-job-contract-feedback-added? receiver)))
      (sendgrid/send-notification-mail receiver-id
                                       (:user/email receiver)
                                       "You received feedback"
                                       body
                                       (:user/name receiver)
                                       "Open Contract"
                                       (u/full-path-for :contract/detail :contract/id contract-id)
                                       :on-job-contract-feedback-added))))

(defn on-job-invitation-added [{:keys [:contract-id :freelancer-id]}]
  (let [contract-id (u/big-num->num contract-id)
        freelancer-id (u/big-num->num freelancer-id)
        contract (ethlance-db/get-contract contract-id [:contract/job :invitation/description] instances)
        job-id (u/big-num->num (:contract/job contract))
        job (ethlance-db/get-job job-id instances)
        freelancer (ethlance-db/get-user (u/big-num->num freelancer-id) instances [:user.notif/disabled-all?
                                                                                   :user.notif/disabled-on-job-invitation-added?])
        body (templates/on-job-invitation-added job contract)]
    (when (and (not (:user.notif/disabled-all? freelancer))
               (not (:user.notif/disabled-on-job-invitation-added? freelancer)))
      (sendgrid/send-notification-mail freelancer-id
                                       (:user/email freelancer)
                                       "You've been invited to apply for a job"
                                       body
                                       (:user/name freelancer)
                                       "Open Job"
                                       (u/full-path-for :job/detail :job/id job-id)
                                       :on-job-invitation-added))))

(defn on-job-contract-message-added [{:keys [:message-id :contract-id :sender-id :receiver-id]}]
  (let [message (ethlance-db/get-message (u/big-num->num message-id) instances)
        sender (ethlance-db/get-user (u/big-num->num sender-id) instances)
        receiver (ethlance-db/get-user (u/big-num->num receiver-id) instances [:user.notif/disabled-all?
                                                                               :user.notif/disabled-on-message-added?])
        body (templates/on-job-contract-message-added message sender)]
    (when (and (not (:user.notif/disabled-all? receiver))
               (not (:user.notif/disabled-on-message-added? receiver)))
      (sendgrid/send-notification-mail receiver-id
                                       (:user/email receiver)
                                       "You received message"
                                       body
                                       (:user/name receiver)
                                       "Open Contract"
                                       (u/full-path-for :contract/detail :contract/id (u/big-num->num contract-id))
                                       :on-job-contract-message-added))))

(def users-job-recommendation-limit 200)

(defn on-job-added [{:keys [:job-id]}]
  (let [job (-> (ethlance-db/get-job job-id instances [:job/skills-count :job/title :job/description :job/category])
              (assoc :job/id job-id))
        job-skills (:job/skills (ethlance-db/get-job-skills job-id (:job/skills-count job) instances))]
    (loop [offset 0]
      (let [user-ids (ethlance-db/search-freelancers-by-any-of-skills (:job/category job)
                                                                      job-skills
                                                                      1
                                                                      offset
                                                                      users-job-recommendation-limit
                                                                      instances)]
        (u/log! "on-job-added" (str job-id) "freelancers" (count user-ids))
        (doseq [user-id user-ids]
          (let [user (ethlance-db/get-user user-id instances)
                body (templates/on-job-added [job])]
            (sendgrid/send-notification-mail user-id
                                             (:user/email user)
                                             "We have a new job for you!"
                                             body
                                             (:user/name user)
                                             "Find Work"
                                             (u/full-path-for :search/jobs)
                                             :on-job-added)))
        (when (= (count user-ids) users-job-recommendation-limit)
          (recur (+ offset users-job-recommendation-limit)))))))

(defn on-job-recommendation-interval [min-created-on job-recommendations]
  (let [jobs (->> (ethlance-db/search-jobs min-created-on 0 10000 instances)
               (reduce (fn [acc job-id]
                         (let [job (ethlance-db/get-job job-id
                                                        instances
                                                        [:job/skills-count :job/title :job/description :job/category])
                               job-skills (ethlance-db/get-job-skills job-id (:job/skills-count job) instances)
                               freelancers (ethlance-db/search-freelancers-by-any-of-skills (:job/category job)
                                                                                            (:job/skills job-skills)
                                                                                            job-recommendations
                                                                                            0
                                                                                            10000
                                                                                            instances)]
                           (assoc acc job-id (-> job
                                               (assoc :job/id job-id)
                                               (merge job-skills)
                                               (assoc :job/matching-freelancers (set freelancers))))))
                       {}))
        all-freelancers-ids (distinct (flatten (vals (medley/map-vals (comp vec :job/matching-freelancers) jobs))))]
    (u/log! "Scheduler" job-recommendations "found" (count jobs) "jobs" (count all-freelancers-ids) "freelancers"
            "since" (str (new js/Date (* min-created-on 1000))) (str "(" min-created-on ")"))
    (doseq [user-id all-freelancers-ids]
      (let [user (ethlance-db/get-user user-id instances)
            recommended-jobs (filter #(contains? (:job/matching-freelancers %) user-id) (vals jobs))
            body (templates/on-job-recommendations-interval recommended-jobs)]
        (when (pos? (count recommended-jobs))
          (sendgrid/send-notification-mail user-id
                                           (:user/email user)
                                           "Job recommendations"
                                           body
                                           (:user/name user)
                                           "Find Work"
                                           (u/full-path-for :search/jobs)
                                           :on-job-recommendation-interval))))))

(def min-created-on-fns
  {2 (partial u/hours-ago-from-now 12)
   3 (partial u/days-ago-from-now 1)
   4 (partial u/days-ago-from-now 3)
   5 (partial u/days-ago-from-now 7)})

(defn setup-scheduler! [job-recommmendations]
  (.scheduleJob schedule
                (constants/job-recommendations-cron job-recommmendations)
                (fn []
                  (u/log! "Scheduler" job-recommmendations "was fired")
                  (on-job-recommendation-interval (u/get-time-without-milis ((min-created-on-fns job-recommmendations)))
                                                  job-recommmendations))))

(defn on-freelancer-added [{:keys [:user-id]}]
  (.log js/console "Freelancer added" (str user-id)))

(defn on-employer-added [{:keys [:user-id]}]
  (.log js/console "Employer added" (str user-id)))

(def ^:dynamic sched-job)

(comment
  (type Web3)
  (do
    (setup-listener! :ethlance-invoice :on-invoice-added on-invoice-added)
    (setup-listener! :ethlance-invoice :on-invoice-paid on-invoice-paid)
    (setup-listener! :ethlance-invoice :on-invoice-cancelled on-invoice-cancelled)
    (setup-listener! :ethlance-contract :on-job-proposal-added on-job-proposal-added)
    (setup-listener! :ethlance-contract :on-job-contract-added on-job-contract-added)
    (setup-listener! :ethlance-contract :on-job-contract-cancelled on-job-contract-cancelled)
    (setup-listener! :ethlance-contract :on-job-contract-feedback-added on-job-contract-feedback-added)
    (setup-listener! :ethlance-contract :on-job-invitation-added on-job-invitation-added)
    (setup-listener! :ethlance-message :on-job-contract-message-added on-job-contract-message-added)
    (setup-listener! :ethlance-job :on-job-added on-job-added)
    (setup-listener! :ethlance-user :on-freelancer-added on-freelancer-added)
    (setup-listener! :ethlance-user :on-employer-added on-employer-added))
  (sendgrid/send-notification-mail "matus.lestan@ethlance.com"
                                   "test"
                                   "asdnaskjdnakjsdnka</br></br>asjdknakjsdnajksd"
                                   "testname"
                                   "testbutotn"
                                   "http://ethlance.com"
                                   :test-email)
  (on-job-added {:job-id 10})
  (set! sched-job (.scheduleJob schedule "* * * * *" (fn []
                                                       (println "test"))))
  (.cancel sched-job)
  (u/days-ago-from-now 1)
  (u/hours-ago-from-now 12)
  (set! sched-job (setup-scheduler! 2))
  (on-job-recommendation-interval 1457049600 0)
  (ethlance-db/get-user 1 instances)
  (on-job-added {:job-id 45})
  (ethlance-db/get-entities [1] [:user/name :user/email] (:ethlance-db instances)))

(defn -main [& _]
  (setup-listener! :ethlance-invoice :on-invoice-added on-invoice-added)
  (setup-listener! :ethlance-invoice :on-invoice-paid on-invoice-paid)
  (setup-listener! :ethlance-invoice :on-invoice-cancelled on-invoice-cancelled)
  (setup-listener! :ethlance-contract :on-job-proposal-added on-job-proposal-added)
  (setup-listener! :ethlance-contract :on-job-contract-added on-job-contract-added)
  (setup-listener! :ethlance-contract :on-job-contract-cancelled on-job-contract-cancelled)
  (setup-listener! :ethlance-contract :on-job-contract-feedback-added on-job-contract-feedback-added)
  (setup-listener! :ethlance-contract :on-job-invitation-added on-job-invitation-added)
  (setup-listener! :ethlance-message :on-job-contract-message-added on-job-contract-message-added)
  (setup-listener! :ethlance-job :on-job-added on-job-added)
  (setup-listener! :ethlance-user :on-freelancer-added on-freelancer-added)
  (setup-listener! :ethlance-user :on-employer-added on-employer-added)
  (doseq [job-recommendations (keys constants/job-recommendations-cron)]
    (setup-scheduler! job-recommendations))
  (.log js/console "Listeners have been setup" (aget nodejs/process "env" "WEB3_URL")))

(set! *main-cli-fn* -main)